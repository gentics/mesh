package com.gentics.mesh.rest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.core.verticle.admin.AdminHandler;
import com.gentics.mesh.core.verticle.auth.AuthenticationRestHandler;
import com.gentics.mesh.core.verticle.group.GroupCrudHandler;
import com.gentics.mesh.core.verticle.microschema.MicroschemaCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;
import com.gentics.mesh.core.verticle.project.ProjectCrudHandler;
import com.gentics.mesh.core.verticle.role.RoleCrudHandler;
import com.gentics.mesh.core.verticle.schema.SchemaContainerCrudHandler;
import com.gentics.mesh.core.verticle.tag.TagCrudHandler;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyCrudHandler;
import com.gentics.mesh.core.verticle.user.UserCrudHandler;
import com.gentics.mesh.core.verticle.utility.UtilityHandler;
import com.gentics.mesh.core.verticle.webroot.WebRootHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

/**
 * Local client implementation.
 */
@Component
public class MeshRestLocalClientImpl implements MeshRestClient {

	private MeshAuthUser user;

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	private Database database;

	@Autowired
	private UserCrudHandler userCrudHandler;

	@Autowired
	private RoleCrudHandler roleCrudHandler;

	@Autowired
	private GroupCrudHandler groupCrudHandler;

	@Autowired
	private SchemaContainerCrudHandler schemaCrudHandler;

	@Autowired
	private MicroschemaCrudHandler microschemaCrudHandler;

	@Autowired
	private TagCrudHandler tagCrudHandler;

	@Autowired
	private TagFamilyCrudHandler tagFamilyCrudHandler;

	@Autowired
	private ProjectCrudHandler projectCrudHandler;

	@Autowired
	private NodeCrudHandler nodeCrudHandler;

	@Autowired
	private NodeFieldAPIHandler fieldAPIHandler;

	@Autowired
	private WebRootHandler webrootHandler;

	@Autowired
	private AdminHandler adminHandler;

	@Autowired
	private AuthenticationRestHandler authRestHandler;

	@Autowired
	private UtilityHandler utilityHandler;

	private Map<String, Project> projects = new HashMap<>();

	/**
	 * Set the user which is used for authentication.
	 * 
	 * @param user
	 */
	public void setUser(MeshAuthUser user) {
		this.user = user;
	}

	@Override
	public Future<NodeResponse> findNodeByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(nodeCreateRequest);
		nodeCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
			QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		nodeCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid, String languageTag) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		ac.setQuery("?lang=" + languageTag);
		//TODO set project
		nodeCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodes(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadChildren(ac, parentNodeUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleAddTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRemoveTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();

	}

	@Override
	public Future<GenericMessageResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		nodeCrudHandler.handleMove(ac, nodeUuid, targetFolderUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class, parameters);
		ac.setProject(projectName);
		tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> findTags(String projectName, String tagFamilyUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> findProjectByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class, parameters);
		projectCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> findProjects(QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		projectCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);

		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);

		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> createProject(ProjectCreateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		ac.setPayloadObject(request);
		projectCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		projectCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteProject(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		projectCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameter pagingInfo) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, pagingInfo);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class);
		ac.setPayloadObject(request);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteTagFamily(String projectName, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class);
		ac.setPayloadObject(request);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleUpdate(ac, tagFamilyUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String path, QueryParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(WebRootResponse.class, parameters);
		ac.setProject(projectName);
		//		webrootHandler.handleGetPath(rc);
		return ac.getFuture();
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String[] pathSegments, QueryParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(WebRootResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> createSchema(Schema request) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> findSchemaByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class, parameters);
		schemaCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateSchema(String uuid, Schema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaChangesListModel> diffSchema(String uuid, Schema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleDiff(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteSchema(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		schemaCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> addSchemaToProject(String schemaUuid, String projectUuid) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class);
		schemaCrudHandler.handleAddProjectToSchema(ac, schemaUuid, projectUuid);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> removeSchemaFromProject(String schemaUuid, String projectUuid) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class);
		schemaCrudHandler.handleRemoveProjectFromSchema(ac, projectUuid, schemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> findSchemas(QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		schemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> findSchemas(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		ac.setProject(projectName);
		schemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> findMicroschemas(QueryParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		microschemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(changes);
		microschemaCrudHandler.handleApplySchemaChanges(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> findGroupByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class, parameters);
		groupCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> findGroups(QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		groupCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(groupCreateRequest);
		groupCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(request);
		groupCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteGroup(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		groupCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> findUserByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		userCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> findUserByUsername(String username, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> findUsers(QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		userCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> createUser(UserCreateRequest request, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> updateUser(String uuid, UserUpdateRequest request, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteUser(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		userCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		groupCrudHandler.handleGroupUserList(ac, groupUuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		LocalActionContextImpl<UserPermissionResponse> ac = createContext(UserPermissionResponse.class);
		roleCrudHandler.handlePermissionRead(ac, uuid, pathToElement);
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> findRoleByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class, parameters);
		roleCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> findRoles(QueryParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameter);
		roleCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> createRole(RoleCreateRequest request) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteRole(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		roleCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> findRolesForGroup(String groupUuid, QueryParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameter);

		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		return ac.getFuture();
	}

	@Override
	public Future<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		LocalActionContextImpl<RolePermissionResponse> ac = createContext(RolePermissionResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class);
		ac.setPayloadObject(restRole);
		roleCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Observable<GenericMessageResponse> login() {
		return Observable.just(null);
	}

	@Override
	public Observable<GenericMessageResponse> logout() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.logout();
		return null;
	}

	@Override
	public Future<UserResponse> me() {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class);
		authRestHandler.handleMe(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> searchUsers(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> searchGroups(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> searchRoles(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> searchProjects(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> searchTags(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> searchTagFamilies(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> searchSchemas(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> searchMicroschemas(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SearchStatusResponse> loadSearchStatus() {
		LocalActionContextImpl<SearchStatusResponse> ac = createContext(SearchStatusResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> invokeReindex() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<String> meshStatus() {
		LocalActionContextImpl<String> ac = createContext(String.class);
		//adminHandler.handleStatus(rc);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> schemaMigrationStatus() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> createMicroschema(Microschema request) {
		LocalActionContextImpl<Microschema> ac = createContext(MicroschemaModel.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> findMicroschemaByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<Microschema> ac = createContext(Microschema.class, parameters);
		microschemaCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteMicroschema(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleDiff(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			Buffer fileData, String fileName, String contentType) {

		Vertx vertx = Mesh.vertx();
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);

		Runnable task = () -> {

			File tmpFile = new File(System.getProperty("java.io.tmpdir"), UUIDUtil.randomUUID() + ".upload");
			vertx.fileSystem().writeFileBlocking(tmpFile.getAbsolutePath(), fileData);
			ac.getFileUploads().add(new FileUpload() {

				@Override
				public String uploadedFileName() {
					return tmpFile.getAbsolutePath();
				}

				@Override
				public long size() {
					return fileData.length();
				}

				@Override
				public String name() {
					return fileName;
				}

				@Override
				public String fileName() {
					return fileName;
				}

				@Override
				public String contentType() {
					return contentType;
				}

				@Override
				public String contentTransferEncoding() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String charSet() {
					// TODO Auto-generated method stub
					return null;
				}
			});

			fieldAPIHandler.handleUpdateField(ac, nodeUuid, languageTag, fieldKey);
		};
		new Thread(task).start();
		return ac.getFuture();
	}

	@Override
	public Future<NodeDownloadResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			QueryParameterProvider... parameters) {
		//		LocalActionContextImpl<NodeResponse> ac = createContext();
		//		return ac.getFuture();
		return null;
	}

	@Override
	public Future<GenericMessageResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			ImageManipulationParameter imageManipulationParameter) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<String> resolveLinks(String body, QueryParameterProvider... parameters) {
		LocalActionContextImpl<String> ac = createContext(String.class, parameters);
		//utilityHandler.handleResolveLinks(rc);
		return ac.getFuture();
	}

	@Override
	public Future<NavigationResponse> loadNavigation(String projectName, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NavigationResponse> ac = createContext(NavigationResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<NavigationResponse> navroot(String projectName, String path, QueryParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void eventbus(Handler<WebSocket> wsConnect) {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpClient getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient setLogin(String username, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient initializeAuthenticationProvider(RoutingContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	/**
	 * Create a new local action context using the provides parameters.
	 * 
	 * @param responseType
	 * @param parameters
	 *            Parameters which will be transformed to query parameters
	 * @return
	 */
	private <T> LocalActionContextImpl<T> createContext(Class<? extends T> responseType, QueryParameterProvider... parameters) {
		LocalActionContextImpl<T> ac = new LocalActionContextImpl<>(user, responseType, parameters);
		return ac;
	}

	public void addProject(String name, Project project) {
		this.projects.put(name, project);
	}

}

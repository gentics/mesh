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
import com.gentics.mesh.core.rest.MeshServerInfoModel;
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
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReferenceList;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
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
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import rx.Single;

/**
 * Local client implementation. This client will invoke endpoint handlers instead of sending http rest requests.
 */
@Component
public class MeshLocalClientImpl implements MeshRestClient {

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
	public Future<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(nodeCreateRequest);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		ac.setProject(projectName);
		ac.setQuery("?lang=" + languageTag);
		//TODO set project
		nodeCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadChildren(ac, parentNodeUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleAddTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRemoveTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();

	}

	@Override
	public Future<GenericMessageResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleMove(ac, nodeUuid, targetFolderUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
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
	public Future<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters) {
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
	public Future<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class, parameters);
		projectCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class, parameters);
		projectCrudHandler.handleReadByName(ac, name);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> findProjects(ParameterProvider... parameters) {
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
	public Future<Schema> assignSchemaToProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<Schema> ac = createContext(Schema.class);
		ac.setProject(projectName);
		schemaCrudHandler.handleAddSchemaToProject(ac, schemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> unassignSchemaFromProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<Schema> ac = createContext(Schema.class);
		schemaCrudHandler.handleRemoveSchemaFromProject(ac, schemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		schemaCrudHandler.handleReadProjectList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<Microschema> ac = createContext(Microschema.class);
		microschemaCrudHandler.handleAddMicroschemaToProject(ac, microschemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<Microschema> ac = createContext(Microschema.class);
		microschemaCrudHandler.handleRemoveMicroschemaFromProject(ac, microschemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		microschemaCrudHandler.handleReadMicroschemaList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
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
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(WebRootResponse.class, parameters);
		ac.setProject(projectName);
		//		webrootHandler.handleGetPath(rc);
		return ac.getFuture();
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
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
	public Future<Schema> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
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
	public Future<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		schemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
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
	public Future<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class, parameters);
		groupCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> findGroups(ParameterProvider... parameters) {
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
	public Future<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		userCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> findUserByUsername(String username, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> findUsers(ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		userCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> createUser(UserCreateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> updateUser(String uuid, UserUpdateRequest request, ParameterProvider... parameters) {
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
	public Future<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
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
	public Future<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class, parameters);
		roleCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> findRoles(ParameterProvider... parameter) {
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
	public Future<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter) {
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
	public Single<GenericMessageResponse> login() {
		return Single.just(null);
	}

	@Override
	public Single<GenericMessageResponse> logout() {
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
	public Future<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
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
	public Future<Microschema> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
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
			ParameterProvider... parameters) {
		//		LocalActionContextImpl<NodeResponse> ac = createContext();
		//		return ac.getFuture();
		return null;
	}

	@Override
	public Future<GenericMessageResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			ImageManipulationParameters imageManipulationParameter) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<String> resolveLinks(String body, ParameterProvider... parameters) {
		LocalActionContextImpl<String> ac = createContext(String.class, parameters);
		//utilityHandler.handleResolveLinks(rc);
		return ac.getFuture();
	}

	@Override
	public Future<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NavigationResponse> ac = createContext(NavigationResponse.class, parameters);
		ac.setProject(projectName);
		return ac.getFuture();
	}

	@Override
	public Future<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
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
	private <T> LocalActionContextImpl<T> createContext(Class<? extends T> responseType, ParameterProvider... parameters) {
		LocalActionContextImpl<T> ac = new LocalActionContextImpl<>(user, responseType, parameters);
		return ac;
	}

	public void addProject(String name, Project project) {
		this.projects.put(name, project);
	}

	@Override
	public Future<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusResponse> ac = createContext(PublishStatusResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handlePublish(ac, nodeUuid);
		return ac.getFuture();
	}

	@Override
	public Future<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusModel> ac = createContext(PublishStatusModel.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handlePublish(ac, nodeUuid, languageTag);
		return ac.getFuture();
	}

	@Override
	public Future<PublishStatusResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusResponse> ac = createContext(PublishStatusResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleTakeOffline(ac, nodeUuid);
		return ac.getFuture();
	}

	@Override
	public Future<PublishStatusModel> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusModel> ac = createContext(PublishStatusModel.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleTakeOffline(ac, nodeUuid, languageTag);
		return ac.getFuture();
	}

	@Override
	public Future<MeshServerInfoModel> getApiInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ReleaseResponse> createRelease(String projectName, ReleaseCreateRequest releaseCreateRequest, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ReleaseResponse> findReleaseByUuid(String projectName, String releaseUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ReleaseListResponse> findReleases(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ReleaseResponse> updateRelease(String projectName, String releaseUuid, ReleaseUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<SchemaReferenceList> getReleaseSchemaVersions(String projectName, String releaseUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid,
			SchemaReferenceList schemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid,
			SchemaReference... schemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<MicroschemaReferenceList> getReleaseMicroschemaVersions(String projectName, String releaseUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			MicroschemaReferenceList microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			MicroschemaReference... microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

}

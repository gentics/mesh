package com.gentics.mesh.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.gentics.mesh.rest.AbstractMeshRestHttpClient.getQuery;
import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
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
import com.gentics.mesh.core.verticle.group.GroupCrudHandler;
import com.gentics.mesh.core.verticle.microschema.MicroschemaCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeCrudHandler;
import com.gentics.mesh.core.verticle.project.ProjectCrudHandler;
import com.gentics.mesh.core.verticle.role.RoleCrudHandler;
import com.gentics.mesh.core.verticle.schema.SchemaContainerCrudHandler;
import com.gentics.mesh.core.verticle.tag.TagCrudHandler;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyCrudHandler;
import com.gentics.mesh.core.verticle.user.UserCrudHandler;
import com.gentics.mesh.core.verticle.webroot.WebRootHandler;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.PagingParameter;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

/**
 * Local client implementation.
 */
@Component
public class MeshRestLocalClientImpl implements MeshRestClient {

	private MeshAuthUser user;

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
	private WebRootHandler webrootHandler;

	@Autowired
	private AdminHandler adminHandler;

	//@Autowired
	//private Navigation

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext();
		//TODO set project
		ac.setPayloadObject(nodeCreateRequest);
		Future<NodeResponse> future = ac.getFuture();
		ac.setResponseType(NodeResponse.class);
		nodeCrudHandler.handleCreate(ac);
		return future;
	}

	@Override
	public Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
			QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(parameters);
		nodeCrudHandler.handleUpdate(ac, uuid);
		// TODO Auto-generated method stub
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();

		//TODO set project
		Future<GenericMessageResponse> future = ac.getFuture();
		ac.setResponseType(GenericMessageResponse.class);

		nodeCrudHandler.handleDelete(ac, uuid);
		return future;
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid, String languageTag) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();

		ac.setQuery("?lang=" + languageTag);
		//TODO set project
		ac.setResponseType(GenericMessageResponse.class);
		nodeCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodes(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(parameters);
		nodeCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(parameters);

		nodeCrudHandler.handleReadChildren(ac, parentNodeUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(parameters);
		ac.setResponseType(NodeResponse.class);
		ac.setQuery(getQuery(parameters));
		nodeCrudHandler.handleAddTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();
	}

	@Override
	public Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(parameters);
		nodeCrudHandler.handleRemoveTag(ac, nodeUuid, tagUuid);
		return ac.getFuture();

	}

	@Override
	public Future<GenericMessageResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		nodeCrudHandler.handleMove(ac, nodeUuid, targetFolderUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, QueryParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext();
		tagCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagResponse> ac = createContext(parameters);
		tagCrudHandler.handleRead(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext();
		ac.setPayloadObject(request);
		tagCrudHandler.handleUpdate(ac, );
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		//TODO set parameters
		tagCrudHandler.handleDelete(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> findTags(String projectName, String tagFamilyUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext();
		ac.setQuery(getQuery(parameters));
		tagCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> findProjectByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext();
		ac.setQuery(getQuery(parameters));
		projectCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> findProjects(QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext();
		ac.setQuery(getQuery(parameters));
		projectCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext();

		return ac.getFuture();
	}

	@Override
	public Future<ProjectResponse> createProject(ProjectCreateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext();
		ac.setPayloadObject(request);
		Future<ProjectResponse> future = ac.getFuture();
		ac.setResponseType(ProjectResponse.class);
		projectCrudHandler.handleCreate(ac);
		return future;
	}

	@Override
	public Future<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext();
		projectCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteProject(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		projectCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext();
		ac.setQuery(getQuery(parameters));
		tagFamilyCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameter pagingInfo) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext();
		//TODO set project / name
		ac.setQuery(getQuery(pagingInfo));
		tagFamilyCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext();
		ac.setPayloadObject(request);
		tagFamilyCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteTagFamily(String projectName, String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		tagFamilyCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext();
		ac.setPayloadObject(request);
		tagFamilyCrudHandler.handleUpdate(ac, tagFamilyUuid);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String path, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(parameters);
		//		webrootHandler.handleGetPath(rc);
		//		return ac.getFuture();
		return null;
	}

	@Override
	public Future<WebRootResponse> webroot(String projectName, String[] pathSegments, QueryParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> createSchema(Schema request) {
		LocalActionContextImpl<Schema> ac = createContext();
		ac.setPayloadObject(request);
		schemaCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> findSchemaByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<Schema> ac = createContext(parameters);
		schemaCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateSchema(String uuid, Schema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		schemaCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaChangesListModel> diffSchema(String uuid, Schema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteSchema(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		schemaCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> addSchemaToProject(String schemaUuid, String projectUuid) {
		LocalActionContextImpl<Schema> ac = createContext();
		schemaCrudHandler.handleAddProjectToSchema(ac, schemaUuid, projectUuid);
		return ac.getFuture();
	}

	@Override
	public Future<Schema> removeSchemaFromProject(String schemaUuid, String projectUuid) {
		LocalActionContextImpl<Schema> ac = createContext();
		schemaCrudHandler.handleRemoveProjectFromSchema(ac, projectUuid, schemaUuid);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> findSchemas(QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> findSchemas(String projectName, QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(parameters);
		schemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> findMicroschemas(QueryParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(parameters);
		microschemaCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Void> initSchemaStorage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		ac.setPayloadObject(changes);
		microschemaCrudHandler.handleApplySchemaChanges(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> findGroupByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupResponse> ac = createContext(parameters);
		groupCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> findGroups(QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(parameters);
		groupCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		LocalActionContextImpl<GroupResponse> ac = createContext();
		ac.setPayloadObject(groupCreateRequest);
		Future<GroupResponse> future = ac.getFuture();
		ac.setResponseType(GroupResponse.class);
		groupCrudHandler.handleCreate(ac);
		return future;
	}

	@Override
	public Future<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request) {
		LocalActionContextImpl<GroupResponse> ac = createContext();
		ac.setPayloadObject(request);
		groupCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteGroup(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		groupCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext();
		groupCrudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext();
		groupCrudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext();
		groupCrudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		return ac.getFuture();
	}

	@Override
	public Future<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<NodeResponse> ac = createContext();
		groupCrudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		return null;
	}

	@Override
	public Future<UserResponse> findUserByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(parameters);
		userCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> findUserByUsername(String username, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> findUsers(QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(parameters);
		userCrudHandler.handleReadList(ac);
		return ac.getFuture();
	}

	@Override
	public Future<UserResponse> createUser(UserCreateRequest request, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(parameters);
		ac.setPayloadObject(request);
		ac.setResponseType(UserResponse.class);
		userCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	private <T> LocalActionContextImpl<T> createContext(QueryParameterProvider... parameters) {
		LocalActionContextImpl<T> ac = new LocalActionContextImpl<>();
		ac.setQuery(getQuery(parameters));
		ac.setUser(user);
		return ac;
	}

	@Override
	public Future<UserResponse> updateUser(String uuid, UserUpdateRequest request, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteUser(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		userCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		LocalActionContextImpl<UserPermissionResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> findRoleByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<RoleResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> findRoles(QueryParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(parameter);
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> createRole(RoleCreateRequest request) {
		LocalActionContextImpl<RoleResponse> ac = createContext();
		ac.setPayloadObject(request);
		Future<RoleResponse> future = ac.getFuture();
		ac.setResponseType(RoleResponse.class);
		roleCrudHandler.handleCreate(ac);
		return future;
	}

	@Override
	public Future<GenericMessageResponse> deleteRole(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		ac.setResponseType(GenericMessageResponse.class);
		roleCrudHandler.handleDelete(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> findRolesForGroup(String groupUuid, QueryParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(parameter);

		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		LocalActionContextImpl<RolePermissionResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		LocalActionContextImpl<RoleResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Observable<GenericMessageResponse> login() {
		return Observable.just(null);
	}

	@Override
	public Observable<GenericMessageResponse> logout() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		ac.logout();
		return null;
	}

	@Override
	public Future<UserResponse> me() {
		LocalActionContextImpl<UserResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<UserListResponse> searchUsers(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<GroupListResponse> searchGroups(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<RoleListResponse> searchRoles(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<ProjectListResponse> searchProjects(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(parameters);
		//TODO add handler
		return ac.getFuture();
	}

	@Override
	public Future<TagListResponse> searchTags(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<TagFamilyListResponse> searchTagFamilies(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SchemaListResponse> searchSchemas(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<MicroschemaListResponse> searchMicroschemas(String json, QueryParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(parameters);
		return ac.getFuture();
	}

	@Override
	public Future<SearchStatusResponse> loadSearchStatus() {
		LocalActionContextImpl<SearchStatusResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> invokeReindex() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<String> meshStatus() {
		LocalActionContextImpl<NodeResponse> ac = createContext();

		return null;
	}

	@Override
	public Future<GenericMessageResponse> schemaMigrationStatus() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> createMicroschema(Microschema request) {
		LocalActionContextImpl<Microschema> ac = createContext();
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleCreate(ac);
		return ac.getFuture();
	}

	@Override
	public Future<Microschema> findMicroschemaByUuid(String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<Microschema> ac = createContext(parameters);
		microschemaCrudHandler.handleRead(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleUpdate(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> deleteMicroschema(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext();
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleDiff(ac, uuid);
		return ac.getFuture();
	}

	@Override
	public Future<GenericMessageResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			Buffer fileData, String fileName, String contentType) {
		// TODO Auto-generated method stub
		return null;
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
		LocalActionContextImpl<GenericMessageResponse> ac = createContext();
		return ac.getFuture();
	}

	@Override
	public Future<String> resolveLinks(String body, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(parameters);

		return null;
	}

	@Override
	public Future<NavigationResponse> loadNavigation(String projectName, String uuid, QueryParameterProvider... parameters) {
		LocalActionContextImpl<NavigationResponse> ac = createContext();
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
	public ClientSchemaStorage getClientSchemaStorage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient setClientSchemaStorage(ClientSchemaStorage schemaStorage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}

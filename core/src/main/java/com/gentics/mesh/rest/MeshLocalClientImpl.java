package com.gentics.mesh.rest;

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
import com.gentics.mesh.core.rest.node.*;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.role.*;
import com.gentics.mesh.core.rest.schema.*;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.tag.*;
import com.gentics.mesh.core.rest.user.*;
import com.gentics.mesh.core.verticle.admin.AdminHandler;
import com.gentics.mesh.core.verticle.auth.AuthenticationRestHandler;
import com.gentics.mesh.core.verticle.group.GroupCrudHandler;
import com.gentics.mesh.core.verticle.microschema.MicroschemaCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;
import com.gentics.mesh.core.verticle.project.ProjectCrudHandler;
import com.gentics.mesh.core.verticle.release.ReleaseCrudHandler;
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
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.MeshLocalRequestImpl;
import com.gentics.mesh.util.UUIDUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.FileUpload;
import rx.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Local client implementation. This client will invoke endpoint handlers instead of sending http rest requests.
 */
@Singleton
public class MeshLocalClientImpl implements MeshRestClient {

	private MeshAuthUser user;

	private BootstrapInitializer boot;

	private Database database;

	private UserCrudHandler userCrudHandler;

	private RoleCrudHandler roleCrudHandler;

	private GroupCrudHandler groupCrudHandler;

	private SchemaContainerCrudHandler schemaCrudHandler;

	private MicroschemaCrudHandler microschemaCrudHandler;

	private TagCrudHandler tagCrudHandler;

	private TagFamilyCrudHandler tagFamilyCrudHandler;

	private ProjectCrudHandler projectCrudHandler;

	private NodeCrudHandler nodeCrudHandler;

	private NodeFieldAPIHandler fieldAPIHandler;

	private WebRootHandler webrootHandler;

	private AdminHandler adminHandler;

	private AuthenticationRestHandler authRestHandler;

	private UtilityHandler utilityHandler;
	
	private ReleaseCrudHandler releaseCrudHandler;

	@Inject
	public MeshLocalClientImpl(UtilityHandler utilityHandler, AuthenticationRestHandler authRestHandler, AdminHandler adminHandler,
			WebRootHandler webrootHandler, NodeFieldAPIHandler fieldAPIHandler, NodeCrudHandler nodeCrudHandler,
			ProjectCrudHandler projectCrudHandler, TagFamilyCrudHandler tagFamilyCrudHandler, TagCrudHandler tagCrudHandler,
			MicroschemaCrudHandler microschemaCrudHandler, SchemaContainerCrudHandler schemaCrudHandler, GroupCrudHandler groupCrudHandler,
			RoleCrudHandler roleCrudHandler, UserCrudHandler userCrudHandler, Database database, BootstrapInitializer boot, ReleaseCrudHandler releaseCrudHandler) {

		this.utilityHandler = utilityHandler;
		this.authRestHandler = authRestHandler;
		this.adminHandler = adminHandler;
		this.webrootHandler = webrootHandler;
		this.fieldAPIHandler = fieldAPIHandler;
		this.nodeCrudHandler = nodeCrudHandler;
		this.projectCrudHandler = projectCrudHandler;
		this.tagFamilyCrudHandler = tagFamilyCrudHandler;
		this.tagCrudHandler = tagCrudHandler;
		this.microschemaCrudHandler = microschemaCrudHandler;
		this.schemaCrudHandler = schemaCrudHandler;
		this.groupCrudHandler = groupCrudHandler;
		this.roleCrudHandler = roleCrudHandler;
		this.userCrudHandler = userCrudHandler;
		this.database = database;
		this.boot = boot;
		this.releaseCrudHandler = releaseCrudHandler;

	}

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
	public MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(nodeCreateRequest);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
			ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		ac.setQuery("?lang=" + languageTag);
		// TODO set project
		nodeCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadChildren(ac, parentNodeUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleAddTag(ac, nodeUuid, tagUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRemoveTag(ac, nodeUuid, tagUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());

	}

	@Override
	public MeshRequest<Void> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleMove(ac, nodeUuid, targetFolderUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class, parameters);
		ac.setProject(projectName);
		tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		ac.setProject(projectName);
		tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class, parameters);
		projectCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class, parameters);
		projectCrudHandler.handleReadByName(ac, name);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		projectCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);

		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);

		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(ProjectCreateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		ac.setPayloadObject(request);
		projectCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		projectCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteProject(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		projectCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Schema> assignSchemaToProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<Schema> ac = createContext(Schema.class);
		ac.setProject(projectName);
		schemaCrudHandler.handleAddSchemaToProject(ac, schemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> unassignSchemaFromProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		schemaCrudHandler.handleRemoveSchemaFromProject(ac, schemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		schemaCrudHandler.handleReadProjectList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Microschema> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<Microschema> ac = createContext(Microschema.class);
		microschemaCrudHandler.handleAddMicroschemaToProject(ac, microschemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		microschemaCrudHandler.handleRemoveMicroschemaFromProject(ac, microschemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		microschemaCrudHandler.handleReadMicroschemaList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, pagingInfo);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class);
		ac.setPayloadObject(request);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteTagFamily(String projectName, String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest request) {
		LocalActionContextImpl<TagFamilyResponse> ac = createContext(TagFamilyResponse.class);
		ac.setPayloadObject(request);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleUpdate(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		ac.setProject(projectName);
		tagFamilyCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<WebRootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(WebRootResponse.class, parameters);
		ac.setProject(projectName);
		// webrootHandler.handleGetPath(rc);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<WebRootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
		LocalActionContextImpl<WebRootResponse> ac = createContext(WebRootResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Schema> createSchema(Schema request) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Schema> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Schema> ac = createContext(SchemaModel.class, parameters);
		schemaCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, Schema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, Schema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleDiff(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteSchema(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		schemaCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		schemaCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		microschemaCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(changes);
		microschemaCrudHandler.handleApplySchemaChanges(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class, parameters);
		groupCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		groupCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(groupCreateRequest);
		groupCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(request);
		groupCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteGroup(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		groupCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> removeUserFromGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		groupCrudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		groupCrudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> removeRoleFromGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		groupCrudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		userCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserResponse> findUserByUsername(String username, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		userCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserResponse> createUser(UserCreateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteUser(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		userCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		groupCrudHandler.handleGroupUserList(ac, groupUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		LocalActionContextImpl<UserPermissionResponse> ac = createContext(UserPermissionResponse.class);
		roleCrudHandler.handlePermissionRead(ac, uuid, pathToElement);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class, parameters);
		roleCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameter);
		roleCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleResponse> createRole(RoleCreateRequest request) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteRole(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		roleCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameter);

		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		LocalActionContextImpl<RolePermissionResponse> ac = createContext(RolePermissionResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class);
		ac.setPayloadObject(restRole);
		roleCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
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
	public MeshRequest<UserResponse> me() {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class);
		authRestHandler.handleMe(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameters);
		// TODO add handler
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		// TODO add handler
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SearchStatusResponse> loadSearchStatus() {
		LocalActionContextImpl<SearchStatusResponse> ac = createContext(SearchStatusResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeReindex() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> meshStatus() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> schemaMigrationStatus() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Microschema> createMicroschema(Microschema request) {
		LocalActionContextImpl<Microschema> ac = createContext(MicroschemaModel.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Microschema> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Microschema> ac = createContext(Microschema.class, parameters);
		microschemaCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> deleteMicroschema(String uuid) {
		LocalActionContextImpl<Void> ac = createContext(Void.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleDiff(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
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
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeDownloadResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			ParameterProvider... parameters) {
		// LocalActionContextImpl<NodeResponse> ac = createContext();
		// return new MeshLocalRequestImpl<>( ac.getFuture());
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			ImageManipulationParameters imageManipulationParameter) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters) {
		LocalActionContextImpl<String> ac = createContext(String.class, parameters);
		// utilityHandler.handleResolveLinks(rc);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<NavigationResponse> ac = createContext(NavigationResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
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
	public MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusResponse> ac = createContext(PublishStatusResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handlePublish(ac, nodeUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		LocalActionContextImpl<PublishStatusModel> ac = createContext(PublishStatusModel.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handlePublish(ac, nodeUuid, languageTag);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleTakeOffline(ac, nodeUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<Void> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters) {
		LocalActionContextImpl<Void> ac = createContext(Void.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleTakeOffline(ac, nodeUuid, languageTag);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MeshServerInfoModel> getApiInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ReleaseResponse> createRelease(String projectName, ReleaseCreateRequest releaseCreateRequest,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ReleaseResponse> findReleaseByUuid(String projectName, String releaseUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ReleaseListResponse> findReleases(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<ReleaseListResponse> ac = createContext(ReleaseListResponse.class, parameters);
		ac.setProject(projectName);
		releaseCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ReleaseResponse> updateRelease(String projectName, String releaseUuid, ReleaseUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaReferenceList> getReleaseSchemaVersions(String projectName, String releaseUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid,
			SchemaReferenceList schemaVersionReferences) {
		LocalActionContextImpl<SchemaReferenceList> ac = createContext(SchemaReferenceList.class);
		ac.setProject(projectName);
		ac.setPayloadObject(schemaVersionReferences);
		releaseCrudHandler.handleAssignSchemaVersion(ac, releaseUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}
	@Override
	public MeshRequest<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid,
			SchemaReference... schemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaReferenceList> getReleaseMicroschemaVersions(String projectName, String releaseUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			MicroschemaReferenceList microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			MicroschemaReference... microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

}

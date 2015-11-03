package com.gentics.mesh.rest.impl;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import java.util.Objects;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeBreadcrumbResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
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
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
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
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.rest.AbstractMeshRestClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

public class MeshRestClientImpl extends AbstractMeshRestClient {

	public MeshRestClientImpl(String host, Vertx vertx) {
		this(host, DEFAULT_PORT, vertx);
	}

	public MeshRestClientImpl(String host, int port, Vertx vertx) {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost(host);
		options.setDefaultPort(port);
		this.client = vertx.createHttpClient(options);
	}

	@Override
	public Future<NodeResponse> findNodeByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class);
	}

	@Override
	public Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");
		return invokeRequest(POST, "/" + projectName + "/nodes" + getQuery(parameters), NodeResponse.class, nodeCreateRequest);
	}

	@Override
	public Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
			QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUpdateRequest, "nodeUpdateRequest must not be null");
		return invokeRequest(PUT, "/" + projectName + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class, nodeUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/" + projectName + "/nodes/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<GenericMessageResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(targetFolderUuid, "targetFolderUuid must not be null");
		return invokeRequest(PUT, "/" + projectName + "/nodes/" + nodeUuid + "/moveTo/" + targetFolderUuid, GenericMessageResponse.class);
	}

	@Override
	public Future<NodeListResponse> findNodes(String projectName, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/nodes" + getQuery(parameters), NodeListResponse.class);
	}

	@Override
	public Future<TagListResponse> findTagsForTagFamilies(String projectName, String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/tagFamilies/" + uuid + "/tags" + getQuery(parameters), TagListResponse.class);
	}

	@Override
	public Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/nodes/" + nodeUuid + "/tags" + getQuery(parameters), TagListResponse.class);
	}

	@Override
	public Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(parentNodeUuid, "parentNodeUuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/nodes/" + parentNodeUuid + "/children" + getQuery(parameters), NodeListResponse.class);
	}

	@Override
	public Future<NodeBreadcrumbResponse> loadBreadcrumb(String projectName, String nodeUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/nodes/" + nodeUuid + "/breadcrumb" + getQuery(parameters), NodeBreadcrumbResponse.class);
	}

	@Override
	public Future<TagResponse> createTag(String projectName, TagCreateRequest tagCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagCreateRequest, "tagCreateRequest must not be null");
		return invokeRequest(POST, "/" + projectName + "/tags", TagResponse.class, tagCreateRequest);
	}

	@Override
	public Future<TagResponse> findTagByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/tags/" + uuid + getQuery(parameters), TagResponse.class);
	}

	@Override
	public Future<TagResponse> updateTag(String projectName, String uuid, TagUpdateRequest tagUpdateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagUpdateRequest, "tagUpdateRequest must not be null");
		return invokeRequest(PUT, "/" + projectName + "/tags/" + uuid, TagResponse.class, tagUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteTag(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/" + projectName + "/tags/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/tagFamilies" + getQuery(parameters), TagFamilyListResponse.class);
	}

	@Override
	public Future<TagListResponse> findTags(String projectName, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/tags" + getQuery(parameters), TagListResponse.class);
	}

	// TODO can we actually do this?
	@Override
	public Future<TagResponse> findTagByName(String projectName, String name) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(name, "name must not be null");
		return invokeRequest(GET, "/" + projectName + "/tags/" + name, TagResponse.class);
	}

	@Override
	public Future<NodeListResponse> findNodesForTag(String projectName, String tagUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return invokeRequest(GET, "/" + projectName + "/tags/" + tagUuid + "/nodes" + getQuery(parameters), NodeListResponse.class);
	}

	@Override
	public Future<ProjectResponse> findProjectByUuid(String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/projects/" + uuid + getQuery(parameters), ProjectResponse.class);
	}

	@Override
	public Future<ProjectListResponse> findProjects(QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/projects" + getQuery(parameters), ProjectListResponse.class);
	}

	@Override
	public Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		Objects.requireNonNull(languageUuid, "languageUuid must not be null");
		return invokeRequest(PUT, "/projects/" + projectUuid + "/languages/" + languageUuid, ProjectResponse.class);
	}

	@Override
	public Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		Objects.requireNonNull(languageUuid, "languageUuid must not be null");
		return invokeRequest(DELETE, "/projects/" + projectUuid + "/languages/" + languageUuid, ProjectResponse.class);
	}

	@Override
	public Future<ProjectResponse> createProject(ProjectCreateRequest projectCreateRequest) {
		Objects.requireNonNull(projectCreateRequest, "projectCreateRequest must not be null");
		return invokeRequest(POST, "/projects", ProjectResponse.class, projectCreateRequest);
	}

	@Override
	public Future<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest projectUpdateRequest) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(projectUpdateRequest, "projectUpdateRequest must not be null");
		return invokeRequest(PUT, "/projects/" + uuid, ProjectResponse.class, projectUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteProject(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/projects/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/" + projectName + "/tagFamilies/" + uuid + getQuery(parameters), TagFamilyResponse.class);
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameter pagingInfo) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/tagFamilies" + getQuery(pagingInfo), TagFamilyListResponse.class);
	}

	@Override
	public Future<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest tagFamilyCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyCreateRequest, "tagFamilyCreateRequest must not be null");
		return invokeRequest(POST, "/" + projectName + "/tagFamilies", TagFamilyResponse.class, tagFamilyCreateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteTagFamily(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/" + projectName + "/tagFamilies/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest tagFamilyUpdateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(tagFamilyUpdateRequest, "tagFamilyUpdateRequest must not be null");
		return invokeRequest(PUT, "/" + projectName + "/tagFamilies/" + tagFamilyUuid, TagFamilyResponse.class, tagFamilyUpdateRequest);
	}

	@Override
	public Future<GroupResponse> findGroupByUuid(String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/groups/" + uuid + getQuery(parameters), GroupResponse.class);
	}

	@Override
	public Future<GroupListResponse> findGroups(QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/groups" + getQuery(parameters), GroupListResponse.class);
	}

	@Override
	public Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		Objects.requireNonNull(groupCreateRequest, "groupCreateRequest must not be null");
		return invokeRequest(POST, "/groups", GroupResponse.class, groupCreateRequest);
	}

	@Override
	public Future<GroupResponse> updateGroup(String uuid, GroupUpdateRequest groupUpdateRequest) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(groupUpdateRequest, "groupUpdateRequest must not be null");
		return invokeRequest(PUT, "/groups/" + uuid, GroupResponse.class, groupUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteGroup(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/groups/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<UserResponse> findUserByUuid(String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/users/" + uuid + getQuery(parameters), UserResponse.class);
	}

	@Override
	public Future<UserResponse> findUserByUsername(String username, QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/users/" + username + getQuery(parameters), UserResponse.class);
	}

	@Override
	public Future<UserListResponse> findUsers(QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/users" + getQuery(parameters), UserListResponse.class);
	}

	@Override
	public Future<UserResponse> createUser(UserCreateRequest userCreateRequest, QueryParameterProvider... parameters) {
		Objects.requireNonNull(userCreateRequest, "userCreateRequest must not be null");
		return invokeRequest(POST, "/users" + getQuery(parameters), UserResponse.class, userCreateRequest);
	}

	@Override
	public Future<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest, QueryParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(userUpdateRequest, "userUpdateRequest must not be null");
		return invokeRequest(PUT, "/users/" + uuid + getQuery(parameters), UserResponse.class, userUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteUser(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/users/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<RoleResponse> findRoleByUuid(String uuid, QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/roles/" + uuid + getQuery(parameters), RoleResponse.class);
	}

	@Override
	public Future<RoleListResponse> findRoles(QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/roles" + getQuery(parameters), RoleListResponse.class);
	}

	@Override
	public Future<RoleResponse> createRole(RoleCreateRequest roleCreateRequest) {
		return invokeRequest(POST, "/roles", RoleResponse.class, roleCreateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteRole(String uuid) {
		return invokeRequest(DELETE, "/roles/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<UserResponse> me() {
		return invokeRequest(GET, "/auth/me", UserResponse.class);
	}

	@Override
	public Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return invokeRequest(PUT, "/" + projectName + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(parameters), NodeResponse.class);
	}

	@Override
	public Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return invokeRequest(DELETE, "/" + projectName + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(parameters), NodeResponse.class);
	}

	@Override
	public Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(GET, "/groups/" + groupUuid + "/users" + getQuery(parameters), UserListResponse.class);
	}

	@Override
	public Future<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(PUT, "/groups/" + groupUuid + "/users/" + userUuid, GroupResponse.class);
	}

	@Override
	public Future<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(DELETE, "/groups/" + groupUuid + "/users/" + userUuid, GroupResponse.class);
	}

	@Override
	public Future<RoleListResponse> findRolesForGroup(String groupUuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(GET, "/groups/" + groupUuid + "/roles" + getQuery(parameters), RoleListResponse.class);
	}

	@Override
	public Future<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(PUT, "/groups/" + groupUuid + "/roles/" + roleUuid, GroupResponse.class);
	}

	@Override
	public Future<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return invokeRequest(DELETE, "/groups/" + groupUuid + "/roles/" + roleUuid, GroupResponse.class);
	}

	@Override
	public Future<SchemaResponse> createSchema(SchemaCreateRequest request) {
		return invokeRequest(POST, "/schemas", SchemaResponse.class, request);
	}

	@Override
	public Future<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		return invokeRequest(PUT, "/roles/" + uuid, RoleResponse.class, restRole);
	}

	@Override
	public Future<SchemaResponse> findSchemaByUuid(String uuid, QueryParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(GET, "/schemas/" + uuid + getQuery(parameters), SchemaResponse.class);
	}

	@Override
	public Future<SchemaResponse> updateSchema(String uuid, SchemaUpdateRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(PUT, "/schemas/" + uuid, SchemaResponse.class, request);
	}

	@Override
	public Future<NodeResponse> webroot(String projectName, String path, QueryParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return invokeRequest(GET, "/" + projectName + "/webroot/" + path + getQuery(parameters), NodeResponse.class);
	}

	@Override
	public Future<GenericMessageResponse> deleteSchema(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return invokeRequest(DELETE, "/schemas/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<SchemaResponse> addSchemaToProject(String schemaUuid, String projectUuid) {
		Objects.requireNonNull(schemaUuid, "schemaUuid must not be null");
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		return invokeRequest(PUT, "/schemas/" + schemaUuid + "/projects/" + projectUuid, SchemaResponse.class);
	}

	@Override
	public Future<SchemaListResponse> findSchemas(QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/schemas" + getQuery(parameters), SchemaListResponse.class);
	}

	@Override
	public Future<SchemaListResponse> findSchemas(String projectName, QueryParameterProvider... parameters) {
		return invokeRequest(GET, "/" + projectName + "/schemas" + getQuery(parameters), SchemaListResponse.class);
	}

	@Override
	public Future<SchemaResponse> removeSchemaFromProject(String schemaUuid, String projectUuid) {
		Objects.requireNonNull(schemaUuid, "schemaUuid must not be null");
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		return invokeRequest(DELETE, "/schemas/" + schemaUuid + "/projects/" + projectUuid, SchemaResponse.class);
	}

	@Override
	public Future<GenericMessageResponse> login() {
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(username);
		loginRequest.setPassword(password);
		return invokeRequest(POST, "/auth/login", GenericMessageResponse.class, loginRequest);
	}

	@Override
	public Future<GenericMessageResponse> logout() {
		return invokeRequest(GET, "/auth/logout", GenericMessageResponse.class);
	}

	@Override
	public Future<Void> initSchemaStorage() {
		//TODO handle paging correctly
		Future<SchemaListResponse> schemasFuture = findSchemas(new PagingParameter(1, 100));
		Future<Void> future = Future.future();
		schemasFuture.setHandler(rh -> {
			if (rh.failed()) {
				log.error("Could not load schemas", rh.cause());
				future.fail(rh.cause());
			} else {
				SchemaListResponse list = rh.result();
				for (SchemaResponse schema : list.getData()) {
					getClientSchemaStorage().addSchema(schema);
					log.info("Added schema {" + schema.getName() + "} to schema storage.");
				}
				future.complete();
			}
		});
		return future;
	}

	//		return 
	//		MeshResponseHandler<UserResponse> meshHandler = new MeshResponseHandler<>(UserResponse.class, this);
	//		meshHandler.handle(rh -> {
	//			if (rh.statusCode() == 200) {
	//				setCookie(rh.headers().get("Set-Cookie"));
	//			}
	//		});
	//		HttpClientRequest request = client.get(BASEURI + "/auth/login", meshHandler);
	////		request.headers().add("Authorization", "Basic " + authEnc);
	//		request.headers().add("Accept", "application/json");
	//		request.end();
	//		return meshHandler.getFuture();
	//
	//	}

	@Override
	public Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive) {
		throw new NotImplementedException();
	}

	@Override
	public Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/nodes" + getQuery(parameters), NodeListResponse.class, json);
	}

	@Override
	public Future<UserListResponse> searchUsers(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/users" + getQuery(parameters), UserListResponse.class, json);
	}

	@Override
	public Future<GroupListResponse> searchGroups(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/groups" + getQuery(parameters), GroupListResponse.class, json);
	}

	@Override
	public Future<RoleListResponse> searchRoles(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/roles" + getQuery(parameters), RoleListResponse.class, json);
	}

	@Override
	public Future<MicroschemaListResponse> searchMicroschemas(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/microschemas" + getQuery(parameters), MicroschemaListResponse.class, json);
	}

	@Override
	public Future<ProjectListResponse> searchProjects(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/projects" + getQuery(parameters), ProjectListResponse.class, json);
	}

	@Override
	public Future<TagListResponse> searchTags(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/tags" + getQuery(parameters), TagListResponse.class, json);
	}

	@Override
	public Future<SchemaListResponse> searchSchemas(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/schemas" + getQuery(parameters), SchemaListResponse.class, json);
	}

	@Override
	public Future<TagFamilyListResponse> searchTagFamilies(String json, QueryParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return invokeRequest(POST, "/search/tagFamilies" + getQuery(parameters), TagFamilyListResponse.class, json);
	}

	@Override
	public Future<SearchStatusResponse> loadSearchStatus() {
		return invokeRequest(GET, "/search/status", SearchStatusResponse.class);
	}

	@Override
	public Future<GenericMessageResponse> invokeReindex() {
		return invokeRequest(GET, "/search/reindex", GenericMessageResponse.class);
	}

	@Override
	public Future<String> getMeshStatus() {
		Future<String> future = Future.future();
		String uri = BASEURI + "/admin/status";
		HttpClientRequest request = client.request(HttpMethod.GET, uri, rh -> {
			rh.bodyHandler(bh -> {
				future.complete(bh.toString());
			});
		});
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}
		if (getCookie() != null) {
			request.headers().add("Cookie", getCookie());
		} else {
			request.headers().add("Authorization", "Basic " + authEnc);
		}
		request.headers().add("Accept", "application/json");
		request.end();
		return future;
	}

	@Override
	public Future<GenericMessageResponse> updateNodeBinaryField(String projectName, String nodeUuid, Buffer fileData, String fileName,
			String contentType) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fileData, "fileData must not be null");
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(contentType, "contentType must not be null");

		//TODO handle escaping of filename
		String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
		Buffer multiPartFormData = Buffer.buffer();
		String header = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + "someName" + "\"; filename=\"" + fileName + "\"\r\n"
				+ "Content-Type: " + contentType + "\r\n" + "Content-Transfer-Encoding: binary\r\n" + "\r\n";
		multiPartFormData.appendString(header);
		multiPartFormData.appendBuffer(fileData);
		String footer = "\r\n--" + boundary + "--\r\n";
		multiPartFormData.appendString(footer);
		String bodyContentType = "multipart/form-data; boundary=" + boundary;
		return invokeRequest(POST, "/" + projectName + "/nodes/" + nodeUuid + "/bin", GenericMessageResponse.class, multiPartFormData,
				bodyContentType);
	}

	@Override
	public Future<NodeDownloadResponse> downloadBinaryField(String projectName, String nodeUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");

		Future<NodeDownloadResponse> future = Future.future();
		String path = "/" + projectName + "/nodes/" + nodeUuid + "/bin";
		String uri = BASEURI + path;

		HttpClientRequest request = client.request(GET, uri, rh -> {
			NodeDownloadResponse response = new NodeDownloadResponse();
			String contentType = rh.getHeader(HttpHeaders.CONTENT_TYPE.toString());
			response.setContentType(contentType);
			String disposition = rh.getHeader("content-disposition");
			String filename = disposition.substring(disposition.indexOf("=") + 1);
			response.setFilename(filename);
			rh.bodyHandler(buffer -> {
				response.setBuffer(buffer);
				future.complete(response);
			});
		});
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}

		if (getCookie() != null) {
			request.headers().add("Cookie", getCookie());
		} else {
			request.headers().add("Authorization", "Basic " + authEnc);
		}
		request.headers().add("Accept", "application/json");

		request.end();
		return future;
	}

	@Override
	public Future<GenericMessageResponse> updateRolePermission(String roleUuid, String pathToElement, RolePermissionRequest request) {
		Objects.requireNonNull(roleUuid, "roleUuid must not be null");
		Objects.requireNonNull(pathToElement, "pathToElement must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return invokeRequest(PUT, "/roles/" + roleUuid + "/permissions/" + pathToElement, GenericMessageResponse.class, request);
	}

	@Override
	public Future<RolePermissionResponse> readRolePermission(String roleUuid, String pathToElement) {
		Objects.requireNonNull(roleUuid, "roleUuid must not be null");
		Objects.requireNonNull(pathToElement, "pathToElement must not be null");
		return invokeRequest(GET, "/roles/" + roleUuid + "/permissions/" + pathToElement, RolePermissionResponse.class);
	}

}

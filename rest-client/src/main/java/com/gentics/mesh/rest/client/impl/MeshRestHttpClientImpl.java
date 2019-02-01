package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
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
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestRequestUtil;
import com.gentics.mesh.util.URIUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

/**
 * HTTP based REST client implementation.
 */
public abstract class MeshRestHttpClientImpl extends AbstractMeshRestHttpClient {

	@Override
	public MeshRestClient enableAnonymousAccess() {
		disableAnonymousAccess = false;
		return this;
	}

	@Override
	public MeshRestClient disableAnonymousAccess() {
		disableAnonymousAccess = true;
		return this;
	}

	@Override
	public MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsertRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUpsertRequest, "nodeUpsertRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class,
			nodeUpsertRequest);
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes" + getQuery(parameters), NodeResponse.class, nodeCreateRequest);
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class,
			nodeCreateRequest);
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUpdateRequest, "nodeUpdateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(parameters), NodeResponse.class,
			nodeUpdateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/languages/" + languageTag + getQuery(parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(targetFolderUuid, "targetFolderUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/moveTo/" + targetFolderUuid + getQuery(parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes" + getQuery(parameters), NodeListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags" + getQuery(parameters),
			TagListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags" + getQuery(parameters), TagListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags" + getQuery(parameters), TagListResponse.class,
			request);
	}

	@Override
	public MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(parentNodeUuid, "parentNodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + parentNodeUuid + "/children" + getQuery(parameters),
			NodeListResponse.class);
	}

	@Override
	public MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/navigation" + getQuery(parameters),
			NavigationResponse.class);
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest tagCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(tagCreateRequest, "tagCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags", TagResponse.class,
			tagCreateRequest);
	}

	@Override
	public MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid + getQuery(parameters),
			TagResponse.class);
	}

	@Override
	public MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest tagUpdateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagUpdateRequest, "tagUpdateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid, TagResponse.class,
			tagUpdateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies" + getQuery(parameters), TagFamilyListResponse.class);
	}

	// // TODO can we actually do this?
	// @Override
	// public MeshRequest<TagResponse> findTagByName(String projectName, String name) {
	// Objects.requireNonNull(projectName, "projectName must not be null");
	// Objects.requireNonNull(name, "name must not be null");
	// return invokeRequest(GET, "/" + encodeFragment(projectName) + "/tags/" + name, TagResponse.class);
	// }

	@Override
	public MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + tagUuid + "/nodes" + getQuery(
			parameters), NodeListResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/projects/" + uuid + getQuery(parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		Objects.requireNonNull(name, "name must not be null");
		return prepareRequest(GET, "/" + encodeSegment(name) + getQuery(parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters) {
		return prepareRequest(GET, "/projects" + getQuery(parameters), ProjectListResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		Objects.requireNonNull(languageUuid, "languageUuid must not be null");
		return prepareRequest(POST, "/projects/" + projectUuid + "/languages/" + languageUuid, ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		Objects.requireNonNull(projectUuid, "projectUuid must not be null");
		Objects.requireNonNull(languageUuid, "languageUuid must not be null");
		return prepareRequest(DELETE, "/projects/" + projectUuid + "/languages/" + languageUuid, ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(ProjectCreateRequest projectCreateRequest) {
		Objects.requireNonNull(projectCreateRequest, "projectCreateRequest must not be null");
		return prepareRequest(POST, "/projects", ProjectResponse.class, projectCreateRequest);
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(String uuid, ProjectCreateRequest projectCreateRequest) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(projectCreateRequest, "projectCreateRequest must not be null");
		return prepareRequest(POST, "/projects/" + uuid, ProjectResponse.class, projectCreateRequest);
	}

	@Override
	public MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest projectUpdateRequest) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(projectUpdateRequest, "projectUpdateRequest must not be null");
		return prepareRequest(POST, "/projects/" + uuid, ProjectResponse.class, projectUpdateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteProject(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/projects/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(schemaUuid, "schemaUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/schemas/" + schemaUuid, SchemaResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> unassignSchemaFromProject(String projectName, String schemaUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(schemaUuid, "schemaUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/schemas/" + schemaUuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/schemas" + getQuery(parameters), SchemaListResponse.class);
	}

	@Override
	public MeshRequest<MicroschemaResponse> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(microschemaUuid, "microschemaUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/microschemas/" + microschemaUuid, MicroschemaResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(microschemaUuid, "microschemaUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/microschemas/" + microschemaUuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/microschemas" + getQuery(parameters), MicroschemaListResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + uuid + getQuery(parameters), TagFamilyResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies" + getQuery(pagingInfo), TagFamilyListResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest tagFamilyCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyCreateRequest, "tagFamilyCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies", TagFamilyResponse.class, tagFamilyCreateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTagFamily(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/tagFamilies/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest tagFamilyUpdateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(tagFamilyUpdateRequest, "tagFamilyUpdateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid, TagFamilyResponse.class,
			tagFamilyUpdateRequest);
	}

	@Override
	public MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/groups/" + uuid + getQuery(parameters), GroupResponse.class);
	}

	@Override
	public MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters) {
		return prepareRequest(GET, "/groups" + getQuery(parameters), GroupListResponse.class);
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		Objects.requireNonNull(groupCreateRequest, "groupCreateRequest must not be null");
		return prepareRequest(POST, "/groups", GroupResponse.class, groupCreateRequest);
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(String uuid, GroupCreateRequest groupCreateRequest) {
		Objects.requireNonNull(uuid, "The group uuid must not be null");
		Objects.requireNonNull(groupCreateRequest, "groupCreateRequest must not be null");
		return prepareRequest(POST, "/groups/" + uuid, GroupResponse.class, groupCreateRequest);
	}

	@Override
	public MeshRequest<GroupResponse> updateGroup(String uuid, GroupUpdateRequest groupUpdateRequest) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(groupUpdateRequest, "groupUpdateRequest must not be null");
		return prepareRequest(POST, "/groups/" + uuid, GroupResponse.class, groupUpdateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteGroup(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/groups/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/users/" + uuid + getQuery(parameters), UserResponse.class);
	}

	@Override
	public MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters) {
		return prepareRequest(GET, "/users" + getQuery(parameters), UserListResponse.class);
	}

	@Override
	public MeshRequest<UserResponse> createUser(UserCreateRequest userCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(userCreateRequest, "userCreateRequest must not be null");
		return prepareRequest(POST, "/users" + getQuery(parameters), UserResponse.class, userCreateRequest);
	}

	@Override
	public MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest userCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(userCreateRequest, "userCreateRequest must not be null");
		return prepareRequest(POST, "/users/" + uuid + getQuery(parameters), UserResponse.class, userCreateRequest);
	}

	@Override
	public MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(userUpdateRequest, "userUpdateRequest must not be null");
		return prepareRequest(POST, "/users/" + uuid + getQuery(parameters), UserResponse.class, userUpdateRequest);
	}

	@Override
	public MeshRequest<UserResetTokenResponse> getUserResetToken(String userUuid) {
		Objects.requireNonNull(userUuid, "userUuid must not be null");
		return prepareRequest(POST, "/users/" + userUuid + "/reset_token", UserResetTokenResponse.class);
	}

	@Override
	public MeshRequest<UserAPITokenResponse> issueAPIToken(String userUuid) {
		Objects.requireNonNull(userUuid, "userUuid must not be null");
		return prepareRequest(POST, "/users/" + userUuid + "/token", UserAPITokenResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invalidateAPIToken(String userUuid) {
		Objects.requireNonNull(userUuid, "userUuid must not be null");
		return prepareRequest(DELETE, "/users/" + userUuid + "/token", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteUser(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/users/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(pathToElement, "pathToElement must not be null");
		return prepareRequest(GET, "/users/" + uuid + "/permissions/" + pathToElement, UserPermissionResponse.class);
	}

	@Override
	public MeshRequest<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters) {
		return prepareRequest(GET, "/roles/" + uuid + getQuery(parameters), RoleResponse.class);
	}

	@Override
	public MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameters) {
		return prepareRequest(GET, "/roles" + getQuery(parameters), RoleListResponse.class);
	}

	@Override
	public MeshRequest<RoleResponse> createRole(RoleCreateRequest createRequest) {
		return prepareRequest(POST, "/roles", RoleResponse.class, createRequest);
	}

	@Override
	public MeshRequest<RoleResponse> createRole(String uuid, RoleCreateRequest createRequest) {
		return prepareRequest(POST, "/roles/" + uuid, RoleResponse.class, createRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteRole(String uuid) {
		return prepareRequest(DELETE, "/roles/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<UserResponse> me() {
		return prepareRequest(GET, "/auth/me", UserResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(parameters),
			NodeResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(parameters),
			PublishStatusResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(parameters),
			PublishStatusResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published" + getQuery(
			parameters), PublishStatusModel.class);
	}

	@Override
	public MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published" + getQuery(
			parameters), PublishStatusModel.class);
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published"
			+ getQuery(parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(GET, "/groups/" + groupUuid + "/users" + getQuery(parameters), UserListResponse.class);
	}

	@Override
	public MeshRequest<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(POST, "/groups/" + groupUuid + "/users/" + userUuid, GroupResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(DELETE, "/groups/" + groupUuid + "/users/" + userUuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(GET, "/groups/" + groupUuid + "/roles" + getQuery(parameters), RoleListResponse.class);
	}

	@Override
	public MeshRequest<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(POST, "/groups/" + groupUuid + "/roles/" + roleUuid, GroupResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(DELETE, "/groups/" + groupUuid + "/roles/" + roleUuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request) {
		return prepareRequest(POST, "/schemas", SchemaResponse.class, request);
	}

	@Override
	public MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		return prepareRequest(POST, "/roles/" + uuid, RoleResponse.class, restRole);
	}

	@Override
	public MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/schemas/" + uuid + getQuery(parameters), SchemaResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + getQuery(parameters), GenericMessageResponse.class, request);
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateSchema(Schema schema) {
		return prepareRequest(POST, "/utilities/validateSchema", SchemaValidationResponse.class, schema);
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateMicroschema(Microschema schema) {
		return prepareRequest(POST, "/utilities/validateMicroschema", SchemaValidationResponse.class, schema);
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, Schema request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + "/diff", SchemaChangesListModel.class, request);
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + "/diff", SchemaChangesListModel.class, request);
	}

	@Override
	public MeshRequest<WebRootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return webroot(projectName, path.split("/"), parameters);
	}

	@Override
	public MeshRequest<WebRootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
//		Objects.requireNonNull(projectName, "projectName must not be null");
//		Objects.requireNonNull(pathSegments, "pathSegments must not be null");
//
//		String path = Arrays.stream(pathSegments)
//			.filter(segment -> segment != null && !segment.isEmpty())
//			.map(URIUtils::encodeSegment)
//			.collect(Collectors.joining("/", "/", ""));
//
//		String requestUri = getBaseUri() + "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(parameters);
//		ResponseHandler<WebRootResponse> handler = new WebRootResponseHandler(HttpMethod.GET, requestUri);
//		HttpClientRequest request = getClient().request(GET, requestUri, handler);
//
//		return new MeshHttpRequestImpl<>(request, handler, null, null, authentication, "*/*");
		// TODO Implement
		throw new RuntimeException("Not implemented");

	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String path, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return webrootUpdate(projectName, path.split("/"), nodeUpdateRequest, parameters);
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String[] pathSegments, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(pathSegments, "pathSegments must not be null");
		Objects.requireNonNull(nodeUpdateRequest, "nodeUpdateRequest must not be null");

		String path = Arrays.stream(pathSegments)
			.filter(segment -> segment != null && !segment.isEmpty())
			.map(URIUtils::encodeSegment)
			.collect(Collectors.joining("/", "/", ""));

		String requestUri = "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(parameters);
		return prepareRequest(POST, requestUri, NodeResponse.class, nodeUpdateRequest);
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String path, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return webrootCreate(projectName, path.split("/"), nodeCreateRequest , parameters);
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String[] pathSegments, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(pathSegments, "pathSegments must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");

		String path = Arrays.stream(pathSegments)
			.filter(segment -> segment != null && !segment.isEmpty())
			.map(URIUtils::encodeSegment)
			.collect(Collectors.joining("/", "/", ""));

		String requestUri = "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(parameters);
		return prepareRequest(POST, requestUri, NodeResponse.class, nodeCreateRequest);
	}

	@Override
	public MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		String requestUri = "/" + encodeSegment(projectName) + "/navroot" + path + getQuery(parameters);
		return prepareRequest(GET, requestUri, NavigationResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteSchema(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/schemas/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		return prepareRequest(GET, "/schemas" + getQuery(parameters), SchemaListResponse.class);
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/nodes" + getQuery(parameters), NodeListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchNodesRaw(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/nodes" + getQuery(parameters), JsonObject.class, json);
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/nodes" + getQuery(parameters), NodeListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchNodesRaw(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/nodes" + getQuery(parameters), JsonObject.class, json);
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/users" + getQuery(parameters), UserListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchUsersRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/users", JsonObject.class, json);
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/groups" + getQuery(parameters), GroupListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchGroupsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/groups", JsonObject.class, json);
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/roles" + getQuery(parameters), RoleListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchRolesRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/roles", JsonObject.class, json);
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/microschemas" + getQuery(parameters), MicroschemaListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchMicroschemasRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/microschemas", JsonObject.class, json);
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/projects" + getQuery(parameters), ProjectListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchProjectsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/projects", JsonObject.class, json);
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/tags" + getQuery(parameters), TagListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchTagsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/tags", JsonObject.class, json);
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/tags" + getQuery(parameters), TagListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchTagsRaw(String projectName, String json) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/tags", JsonObject.class, json);
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/schemas" + getQuery(parameters), SchemaListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchSchemasRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/schemas", JsonObject.class, json);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/tagFamilies" + getQuery(parameters), TagFamilyListResponse.class, json);
	}

	@Override
	public MeshRequest<JsonObject> searchTagFamiliesRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/tagFamilies", JsonObject.class, json);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/tagFamilies" + getQuery(parameters), TagFamilyListResponse.class,
			json);
	}

	@Override
	public MeshRequest<JsonObject> searchTagFamiliesRaw(String projectName, String json) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/tagFamilies", JsonObject.class, json);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexClear() {
		return prepareRequest(POST, "/search/clear", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexSync() {
		return prepareRequest(POST, "/search/sync", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<SearchStatusResponse> searchStatus() {
		return prepareRequest(GET, "/search/status", SearchStatusResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup() {
		return prepareRequest(POST, "/admin/graphdb/backup", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeExport() {
		return prepareRequest(POST, "/admin/graphdb/export", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeImport() {
		return prepareRequest(POST, "/admin/graphdb/import", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeRestore() {
		return prepareRequest(POST, "/admin/graphdb/restore", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> checkConsistency() {
		return prepareRequest(GET, "/admin/consistency/check", ConsistencyCheckResponse.class);
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> repairConsistency() {
		return prepareRequest(POST, "/admin/consistency/repair", ConsistencyCheckResponse.class);
	}

	@Override
	public MeshRequest<MeshStatusResponse> meshStatus() {
		return prepareRequest(GET, "/admin/status", MeshStatusResponse.class);
	}

	@Override
	public MeshRequest<ClusterStatusResponse> clusterStatus() {
		return prepareRequest(GET, "/admin/cluster/status", ClusterStatusResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version, String fieldKey, byte[] fileData, String fileName, String contentType, ParameterProvider... parameters) {
		return updateNodeBinaryField(projectName, nodeUuid, languageTag, version, fieldKey, new ByteArrayInputStream(fileData), fileData.length, fileName, contentType, parameters);
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version, String fieldKey,
														   InputStream fileData, long fileSize, String fileName, String contentType, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fileData, "fileData must not be null");
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(version, "version must not be null");
		Objects.requireNonNull(contentType, "contentType must not be null");
		if (contentType.isEmpty()) {
			throw new IllegalArgumentException("The contentType of the binary field cannot be empty.");
		}

		// TODO handle escaping of filename
		String boundary = "--------Geg2Oob";
		StringBuilder multiPartFormData = new StringBuilder();

		multiPartFormData.append("--").append(boundary).append("\r\n");
		multiPartFormData.append("Content-Disposition: form-data; name=\"version\"\r\n");
		multiPartFormData.append("\r\n");
		multiPartFormData.append(version).append("\r\n");

		multiPartFormData.append("--").append(boundary).append("\r\n");
		multiPartFormData.append("Content-Disposition: form-data; name=\"language\"\r\n");
		multiPartFormData.append("\r\n");
		multiPartFormData.append(languageTag).append("\r\n");

		multiPartFormData.append("--").append(boundary).append("\r\n");
		multiPartFormData.append("Content-Disposition: form-data; name=\"" + "shohY6d" + "\"; filename=\"").append(fileName).append("\"\r\n");
		multiPartFormData.append("Content-Type: ").append(contentType).append("\r\n");
		multiPartFormData.append("Content-Transfer-Encoding: binary\r\n" + "\r\n");

		InputStream prefix;
		InputStream suffix;
		try {
			prefix = new ByteArrayInputStream(multiPartFormData.toString().getBytes("utf-8"));
			suffix = new ByteArrayInputStream(("\r\n--" + boundary + "--\r\n").getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String bodyContentType = "multipart/form-data; boundary=" + boundary;

		SequenceInputStream completeStream = new SequenceInputStream(
			new SequenceInputStream(
				prefix,
				fileData
			), suffix);

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + getQuery(parameters),
			NodeResponse.class, completeStream, fileSize, bodyContentType);
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
															   ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");

		String path = "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + getQuery(parameters);

		return prepareRequest(GET, path, MeshBinaryResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version,
		String fieldKey, ImageManipulationParameters parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "language must not be null");
		Objects.requireNonNull(version, "version must not be null");
		Objects.requireNonNull(fieldKey, "field key must not be null");

		BinaryFieldTransformRequest transformRequest = new BinaryFieldTransformRequest();
		transformRequest.setCropRect(parameters.getRect());
		transformRequest.setWidth(parameters.getWidth());
		transformRequest.setHeight(parameters.getHeight());
		transformRequest.setLanguage(languageTag).setVersion(version);

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binaryTransform/" + fieldKey, NodeResponse.class,
			transformRequest);
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request) {
		Objects.requireNonNull(roleUuid, "roleUuid must not be null");
		Objects.requireNonNull(pathToElement, "pathToElement must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(POST, "/roles/" + roleUuid + "/permissions/" + pathToElement, GenericMessageResponse.class, request);
	}

	@Override
	public MeshRequest<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		Objects.requireNonNull(roleUuid, "roleUuid must not be null");
		Objects.requireNonNull(pathToElement, "pathToElement must not be null");
		return prepareRequest(GET, "/roles/" + roleUuid + "/permissions/" + pathToElement, RolePermissionResponse.class);
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request) {
		return prepareRequest(POST, "/microschemas", MicroschemaResponse.class, request);
	}

	@Override
	public MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/microschemas/" + uuid + getQuery(parameters), MicroschemaResponse.class);
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
		return prepareRequest(GET, "/microschemas" + getQuery(parameters), MicroschemaListResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + getQuery(parameters), GenericMessageResponse.class, request);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteMicroschema(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/microschemas/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + "/changes", GenericMessageResponse.class, changes);
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + "/changes", GenericMessageResponse.class, changes);
	}

	@Override
	public MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters) {
		Objects.requireNonNull(body, "body must not be null");
		return handleRequest(POST, "/utilities/linkResolver" + getQuery(parameters), String.class, body);
	}

	@Override
	public void eventbus(Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
//		String token = getAuthentication().getToken();
//		if (token != null) {
//			MultiMap headers = MultiMap.caseInsensitiveMultiMap();
//			headers.set("Authorization", "Bearer " + token);
//			getClient().websocket(getBaseUri() + "/eventbus/websocket", headers, wsConnect, failureHandler);
//		} else {
//			getClient().websocket(getBaseUri() + "/eventbus/websocket", wsConnect, failureHandler);
//		}

		// TODO Implement
		throw new RuntimeException("Not implemented");
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchCreateRequest, "branchCreateRequest must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches" + getQuery(parameters), BranchResponse.class,
			branchCreateRequest);
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, String uuid, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(branchCreateRequest, "branchesCreateRequest must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + uuid + getQuery(parameters), BranchResponse.class,
			branchCreateRequest);
	}

	@Override
	public MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + getQuery(parameters), BranchResponse.class);
	}

	@Override
	public MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches" + getQuery(parameters), BranchListResponse.class);
	}

	@Override
	public MeshRequest<BranchResponse> updateBranch(String projectName, String branchUuid, BranchUpdateRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid, BranchResponse.class, request);
	}

	@Override
	public MeshRequest<MeshServerInfoModel> getApiInfo() {
		return prepareRequest(GET, "/", MeshServerInfoModel.class);
	}

	@Override
	public MeshRequest<String> getRAML() {
		return MeshRestRequestUtil.prepareRequest(GET, "/raml", String.class, null, null, this, authentication, disableAnonymousAccess,
			APPLICATION_YAML_UTF8);
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> getBranchSchemaVersions(String projectName, String branchUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/schemas", BranchInfoSchemaList.class);
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
		BranchInfoSchemaList schemaVersionReferences) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/schemas", BranchInfoSchemaList.class,
			schemaVersionReferences);
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
		SchemaReference... schemaVersionReferences) {
		BranchInfoSchemaList info = new BranchInfoSchemaList();
		info.add(schemaVersionReferences);
		return assignBranchSchemaVersions(projectName, branchUuid, info);
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> getBranchMicroschemaVersions(String projectName, String branchUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/microschemas",
			BranchInfoMicroschemaList.class);
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
		BranchInfoMicroschemaList microschemaVersionReferences) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/microschemas",
			BranchInfoMicroschemaList.class, microschemaVersionReferences);
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
		MicroschemaReference... microschemaVersionReferences) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		BranchInfoMicroschemaList list = new BranchInfoMicroschemaList();
		list.add(microschemaVersionReferences);
		return assignBranchMicroschemaVersions(projectName, branchUuid, list);
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchSchemas(String projectName, String branchUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/migrateSchemas", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchMicroschemas(String projectName, String branchUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/migrateMicroschemas",
			GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<BranchResponse> setLatestBranch(String projectName, String branchUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/latest",
			BranchResponse.class);
	}

	@Override
	public MeshRequest<BranchResponse> addTagToBranch(String projectName, String branchUuid, String tagUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/tags/" + tagUuid,
				BranchResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromBranch(String projectName, String branchUuid, String tagUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/tags/" + tagUuid,
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForBranch(String projectName, String branchUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/tags" + getQuery(parameters), TagListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForBranch(String projectName, String branchUuid, TagListUpdateRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/tags", TagListResponse.class,
			request);
	}

	@Override
	public MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(request.getQuery(), "query within the request must not be null");

		String path = "/" + encodeSegment(projectName) + "/graphql" + getQuery(parameters);
		return prepareRequest(POST, path, GraphQLResponse.class, request);
	}

	@Override
	public MeshRequest<JobListResponse> findJobs(PagingParameters... parameters) {
		return prepareRequest(GET, "/admin/jobs" + getQuery(parameters), JobListResponse.class);
	}

	@Override
	public MeshRequest<JobResponse> findJobByUuid(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/admin/jobs/" + uuid, JobResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteJob(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/admin/jobs/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> resetJob(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/admin/jobs/" + uuid + "/error", EmptyResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeJobProcessing() {
		return prepareRequest(POST, "/admin/processJobs", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<PluginResponse> deployPlugin(PluginDeploymentRequest request) {
		Objects.requireNonNull(request, "The deployment request must not be null");
		return prepareRequest(POST, "/admin/plugins", PluginResponse.class, request);
	}

	@Override
	public MeshRequest<GenericMessageResponse> undeployPlugin(String id) {
		Objects.requireNonNull(id, "id must not be null");
		return prepareRequest(DELETE, "/admin/plugins/" + id, GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<PluginListResponse> findPlugins(ParameterProvider... parameters) {
		return prepareRequest(GET, "/admin/plugins" + getQuery(parameters), PluginListResponse.class);
	}

	@Override
	public MeshRequest<PluginResponse> findPlugin(String id) {
		Objects.requireNonNull(id, "id must not be null");
		return prepareRequest(GET, "/admin/plugins/" + id, PluginResponse.class);
	}
}

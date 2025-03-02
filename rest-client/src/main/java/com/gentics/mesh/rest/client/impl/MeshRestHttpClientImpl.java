package com.gentics.mesh.rest.client.impl;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
import static com.gentics.mesh.rest.client.impl.HttpMethod.DELETE;
import static com.gentics.mesh.rest.client.impl.HttpMethod.GET;
import static com.gentics.mesh.rest.client.impl.HttpMethod.POST;
import static com.gentics.mesh.rest.client.impl.HttpMethod.PUT;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.BinaryCheckUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
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
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
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
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.BinaryCheckParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.util.URIUtils;

import io.vertx.core.json.JsonObject;

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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(getConfig(), parameters), NodeResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsertRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUpsertRequest, "nodeUpsertRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(getConfig(), parameters), NodeResponse.class,
			nodeUpsertRequest);
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes" + getQuery(getConfig(), parameters), NodeResponse.class, nodeCreateRequest);
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeCreateRequest, "nodeCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(getConfig(), parameters), NodeResponse.class,
			nodeCreateRequest);
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUpdateRequest, "nodeUpdateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(getConfig(), parameters), NodeResponse.class,
			nodeUpdateRequest);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + uuid + getQuery(getConfig(), parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/languages/" + languageTag + getQuery(getConfig(), parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(targetFolderUuid, "targetFolderUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/moveTo/" + targetFolderUuid + getQuery(getConfig(), parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes" + getQuery(getConfig(), parameters), NodeListResponse.class);
	}

	@Override
	public MeshRequest<NodeVersionsResponse> listNodeVersions(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/versions" + getQuery(getConfig(), parameters),
			NodeVersionsResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags" + getQuery(getConfig(), parameters),
			TagListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags" + getQuery(getConfig(), parameters), TagListResponse.class);
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags" + getQuery(getConfig(), parameters), TagListResponse.class,
			request);
	}

	@Override
	public MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(parentNodeUuid, "parentNodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + parentNodeUuid + "/children" + getQuery(getConfig(), parameters),
			NodeListResponse.class);
	}

	@Override
	public MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/navigation" + getQuery(getConfig(), parameters),
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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid + getQuery(getConfig(), parameters),
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
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, String uuid, TagCreateRequest tagCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagCreateRequest, "tagCreateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid, TagResponse.class,
			tagCreateRequest);
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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies" + getQuery(getConfig(), parameters), TagFamilyListResponse.class);
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
			getConfig(), parameters), NodeListResponse.class);
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "languageUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/languages/" + uuid + getQuery(
			getConfig(), parameters), LanguageResponse.class);
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByTag(String projectName, String tag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tag, "languageTag must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/languages/tag/" + tag + getQuery(
			getConfig(), parameters), LanguageResponse.class);
	}

	@Override
	public MeshRequest<LanguageListResponse> findLanguages(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/languages" + getQuery(getConfig(), parameters), LanguageListResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProjectByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "languageUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/languages/" + uuid + getQuery(
			getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProjectByTag(String projectName, String tag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tag, "languageTag must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/languages/tag/" + tag + getQuery(
			getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProjectByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "languageUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/languages/" + uuid + getQuery(
			getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProjectByTag(String projectName, String tag, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tag, "languageTag must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/languages/tag/" + tag + getQuery(
			getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByTag(String tag, ParameterProvider... parameters) {
		Objects.requireNonNull(tag, "tag must not be null");
		return prepareRequest(GET, "/languages/tag/" + encodeSegment(tag) + getQuery(getConfig(), parameters), LanguageResponse.class);
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/languages/" + uuid + getQuery(getConfig(), parameters), LanguageResponse.class);
	}

	@Override
	public MeshRequest<LanguageListResponse> findLanguages(ParameterProvider... parameters) {
		return prepareRequest(GET, "/languages" + getQuery(getConfig(), parameters), LanguageListResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/projects/" + uuid + getQuery(getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		Objects.requireNonNull(name, "name must not be null");
		return prepareRequest(GET, "/" + encodeSegment(name) + getQuery(getConfig(), parameters), ProjectResponse.class);
	}

	@Override
	public MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters) {
		return prepareRequest(GET, "/projects" + getQuery(getConfig(), parameters), ProjectListResponse.class);
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		return assignLanguageToProjectByUuid(projectUuid, languageUuid);
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		return unassignLanguageFromProjectByUuid(projectUuid, languageUuid);
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
	public MeshRequest<GenericMessageResponse> purgeProject(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/projects/" + uuid + "/maintenance/purge" + getQuery(getConfig(), parameters), GenericMessageResponse.class);
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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/schemas" + getQuery(getConfig(), parameters), SchemaListResponse.class);
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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/microschemas" + getQuery(getConfig(), parameters), MicroschemaListResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + uuid + getQuery(getConfig(), parameters), TagFamilyResponse.class);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies" + getQuery(getConfig(), pagingInfo), TagFamilyListResponse.class);
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
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, String tagFamilyUuid, TagFamilyCreateRequest tagFamilyCreateRequest) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(tagFamilyCreateRequest, "tagFamilyUpdateRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid, TagFamilyResponse.class,
			tagFamilyCreateRequest);
	}

	@Override
	public MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/groups/" + uuid + getQuery(getConfig(), parameters), GroupResponse.class);
	}

	@Override
	public MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters) {
		return prepareRequest(GET, "/groups" + getQuery(getConfig(), parameters), GroupListResponse.class);
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
		return prepareRequest(GET, "/users/" + uuid + getQuery(getConfig(), parameters), UserResponse.class);
	}

	@Override
	public MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters) {
		return prepareRequest(GET, "/users" + getQuery(getConfig(), parameters), UserListResponse.class);
	}

	@Override
	public MeshRequest<UserResponse> createUser(UserCreateRequest userCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(userCreateRequest, "userCreateRequest must not be null");
		return prepareRequest(POST, "/users" + getQuery(getConfig(), parameters), UserResponse.class, userCreateRequest);
	}

	@Override
	public MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest userCreateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(userCreateRequest, "userCreateRequest must not be null");
		return prepareRequest(POST, "/users/" + uuid + getQuery(getConfig(), parameters), UserResponse.class, userCreateRequest);
	}

	@Override
	public MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(userUpdateRequest, "userUpdateRequest must not be null");
		return prepareRequest(POST, "/users/" + uuid + getQuery(getConfig(), parameters), UserResponse.class, userUpdateRequest);
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
		return prepareRequest(GET, "/roles/" + uuid + getQuery(getConfig(), parameters), RoleResponse.class);
	}

	@Override
	public MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameters) {
		return prepareRequest(GET, "/roles" + getQuery(getConfig(), parameters), RoleListResponse.class);
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
	public MeshRequest<UserResponse> me(ParameterProvider... parameters) {
		return prepareRequest(GET, "/auth/me" + getQuery(getConfig(), parameters), UserResponse.class);
	}

	@Override
	public MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(getConfig(), parameters),
			NodeResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(tagUuid, "tagUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/tags/" + tagUuid + getQuery(getConfig(), parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(getConfig(), parameters),
			PublishStatusResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(getConfig(), parameters),
			PublishStatusResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/published" + getQuery(getConfig(), parameters),
			EmptyResponse.class);
	}

	@Override
	public MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published" + getQuery(
			getConfig(), parameters), PublishStatusModel.class);
	}

	@Override
	public MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published" + getQuery(
			getConfig(), parameters), PublishStatusModel.class);
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "languageTag must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/languages/" + languageTag + "/published"
			+ getQuery(getConfig(), parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(groupUuid, "groupUuid must not be null");
		return prepareRequest(GET, "/groups/" + groupUuid + "/users" + getQuery(getConfig(), parameters), UserListResponse.class);
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
		return prepareRequest(GET, "/groups/" + groupUuid + "/roles" + getQuery(getConfig(), parameters), RoleListResponse.class);
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
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request, ParameterProvider... parameters) {
		return prepareRequest(POST, "/schemas" + getQuery(getConfig(), parameters), SchemaResponse.class, request);
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(String uuid, SchemaCreateRequest request, ParameterProvider... parameters) {
		return prepareRequest(POST, "/schemas/" + uuid + getQuery(getConfig(), parameters), SchemaResponse.class, request);
	}

	@Override
	public MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		return prepareRequest(POST, "/roles/" + uuid, RoleResponse.class, restRole);
	}

	@Override
	public MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/schemas/" + uuid + getQuery(getConfig(), parameters), SchemaResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + getQuery(getConfig(), parameters), GenericMessageResponse.class, request);
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateSchema(SchemaModel schema) {
		return prepareRequest(POST, "/utilities/validateSchema", SchemaValidationResponse.class, schema);
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateMicroschema(MicroschemaModel schema) {
		return prepareRequest(POST, "/utilities/validateMicroschema", SchemaValidationResponse.class, schema);
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, SchemaModel request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + "/diff", SchemaChangesListModel.class, request);
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, MicroschemaModel request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + "/diff", SchemaChangesListModel.class, request);
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return webroot(projectName, path.split("/"), parameters);
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(pathSegments, "pathSegments must not be null");

		String path = Arrays.stream(pathSegments)
			.filter(segment -> segment != null && !segment.isEmpty())
			.map(URIUtils::encodeSegment)
			.collect(Collectors.joining("/", "/", ""));

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(getConfig(), parameters), MeshWebrootResponse.class);
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

		String requestUri = "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(getConfig(), parameters);
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
		return webrootCreate(projectName, path.split("/"), nodeCreateRequest, parameters);
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

		String requestUri = "/" + encodeSegment(projectName) + "/webroot" + path + getQuery(getConfig(), parameters);
		return prepareRequest(POST, requestUri, NodeResponse.class, nodeCreateRequest);
	}

	@Override
	public MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		String requestUri = "/" + encodeSegment(projectName) + "/navroot" + path + getQuery(getConfig(), parameters);
		return prepareRequest(GET, requestUri, NavigationResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteSchema(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(DELETE, "/schemas/" + uuid, EmptyResponse.class);
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		return prepareRequest(GET, "/schemas" + getQuery(getConfig(), parameters), SchemaListResponse.class);
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/nodes" + getQuery(getConfig(), parameters), NodeListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/nodes" + getQuery(getConfig(), parameters), ObjectNode.class, json);
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/nodes" + getQuery(getConfig(), parameters), NodeListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/nodes" + getQuery(getConfig(), parameters), ObjectNode.class, json);
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/users" + getQuery(getConfig(), parameters), UserListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchUsersRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/users", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/groups" + getQuery(getConfig(), parameters), GroupListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchGroupsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/groups", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/roles" + getQuery(getConfig(), parameters), RoleListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchRolesRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/roles", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/microschemas" + getQuery(getConfig(), parameters), MicroschemaListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchMicroschemasRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/microschemas", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/projects" + getQuery(getConfig(), parameters), ProjectListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchProjectsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/projects", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/tags" + getQuery(getConfig(), parameters), TagListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/tags", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/tags" + getQuery(getConfig(), parameters), TagListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String projectName, String json) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/tags", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/schemas" + getQuery(getConfig(), parameters), SchemaListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchSchemasRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/schemas", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/search/tagFamilies" + getQuery(getConfig(), parameters), TagFamilyListResponse.class, json);
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String json) {
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/rawSearch/tagFamilies", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/search/tagFamilies" + getQuery(getConfig(), parameters), TagFamilyListResponse.class,
			json);
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String projectName, String json) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(json, "json must not be null");
		return handleRequest(POST, "/" + encodeSegment(projectName) + "/rawSearch/tagFamilies", ObjectNode.class, json);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexClear(ParameterProvider... parameters) {
		return prepareRequest(POST, "/search/clear" + getQuery(getConfig(), parameters), GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexSync(ParameterProvider... parameters) {
		return prepareRequest(POST, "/search/sync" + getQuery(getConfig(), parameters), GenericMessageResponse.class);
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
	public MeshRequest<GenericMessageResponse> invokeBackup(BackupParameters parameters) {
		return prepareRequest(POST, "/admin/graphdb/backup" + getQuery(getConfig(), parameters), GenericMessageResponse.class);
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
	public MeshRequest<ConsistencyCheckResponse> checkConsistency(ParameterProvider... parameters) {
		return prepareRequest(GET, "/admin/consistency/check" + getQuery(getConfig(), parameters), ConsistencyCheckResponse.class);
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> repairConsistency(ParameterProvider... parameters) {
		return prepareRequest(POST, "/admin/consistency/repair" + getQuery(getConfig(), parameters), ConsistencyCheckResponse.class);
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
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version, String fieldKey,
		InputStream fileData, long fileSize, String fileName, String contentType, boolean publish, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fileData, "fileData must not be null");
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(version, "version must not be null");
		Objects.requireNonNull(contentType, "contentType must not be null");
		if (contentType.isEmpty()) {
			throw new IllegalArgumentException("The contentType of the binary field cannot be empty.");
		}

		Map<String, String> fields = new HashMap<>();
		fields.put("version", version);
		fields.put("language", languageTag);
		if (publish) {
			fields.put("publish", "true");
		}
		return prepareFileuploadRequest(POST,
				"/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + getQuery(getConfig(), parameters),
				NodeResponse.class, fileName, contentType, fileData, fileSize, fields);
	}

	@Override
	public MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryUploadRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/s3binary/" + fieldKey + getQuery(getConfig(), parameters),
				S3RestResponse.class, request);
	}

	@Override
	public MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryMetadataRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/s3binary/" + fieldKey + "/parseMetadata" + getQuery(getConfig(), parameters),
				NodeResponse.class, request);
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");

		String path = "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + getQuery(getConfig(), parameters);

		return prepareRequest(GET, path, MeshBinaryResponse.class);
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey, long from,
		long to, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Util.requireNonNegative(from, "from");
		Util.requireNonNegative(to, "to");
		if (to < from) {
			throw new InvalidParameterException(String.format("Parameter to must be equal or greater then from. Given values: %d-%d", from, to));
		}

		String path = "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + getQuery(getConfig(), parameters);

		MeshRequest<MeshBinaryResponse> request = prepareRequest(GET, path, MeshBinaryResponse.class);
		request.setHeader("Range", String.format("bytes=%d-%d", from, to));
		return request;
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
		transformRequest.setCropMode(parameters.getCropMode());
		transformRequest.setResizeMode(parameters.getResizeMode());
		transformRequest.setLanguage(languageTag).setVersion(version);
		if (parameters.hasFocalPoint()) {
			transformRequest.setFocalPoint(parameters.getFocalPoint());
		}

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binaryTransform/" + fieldKey, NodeResponse.class,
			transformRequest);
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryFieldCheckStatus(String projectName, String nodeUuid, String languageTag, String nodeVersion,
			String fieldKey, String secret, String branchUuid, BinaryCheckStatus status, String reason) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(languageTag, "language must not be null");
		Objects.requireNonNull(nodeVersion, "version must not be null");
		Objects.requireNonNull(fieldKey, "field key must not be null");
		Objects.requireNonNull(secret, "secret must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");
		Objects.requireNonNull(status, "status must not be null");

		BinaryCheckUpdateRequest updateRequest = new BinaryCheckUpdateRequest()
			.setStatus(status)
			.setReason(reason);
		ParameterProvider[] parameters = new ParameterProvider[] {
			new NodeParametersImpl().setLanguages(languageTag),
			new VersioningParametersImpl().setVersion(nodeVersion).setBranch(branchUuid),
			new BinaryCheckParametersImpl().setSecret(secret)
		};

		return prepareRequest(
			POST,
			"/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + "/checkCallback" + getQuery(getConfig(), parameters),
			NodeResponse.class,
			updateRequest);
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
	public MeshRequest<MicroschemaResponse> createMicroschema(String uuid, MicroschemaCreateRequest request) {
		return prepareRequest(POST, "/microschemas/" + uuid, MicroschemaResponse.class, request);
	}

	@Override
	public MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/microschemas/" + uuid + getQuery(getConfig(), parameters), MicroschemaResponse.class);
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
		return prepareRequest(GET, "/microschemas" + getQuery(getConfig(), parameters), MicroschemaListResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + getQuery(getConfig(), parameters), GenericMessageResponse.class, request);
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
		return handleTextRequest(POST, "/utilities/linkResolver" + getQuery(getConfig(), parameters), String.class, body);
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchCreateRequest, "branchCreateRequest must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches" + getQuery(getConfig(), parameters), BranchResponse.class,
			branchCreateRequest);
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, String uuid, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(branchCreateRequest, "branchesCreateRequest must not be null");

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + uuid + getQuery(getConfig(), parameters), BranchResponse.class,
			branchCreateRequest);
	}

	@Override
	public MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(branchUuid, "branchUuid must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + getQuery(getConfig(), parameters), BranchResponse.class);
	}

	@Override
	public MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches" + getQuery(getConfig(), parameters), BranchListResponse.class);
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
		MeshRequest<String> request = prepareRequest(GET, "/raml", String.class);
		request.setHeader("Accept", APPLICATION_YAML_UTF8);
		return request;
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
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + branchUuid + "/tags" + getQuery(getConfig(), parameters),
			TagListResponse.class);
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

		String path = "/" + encodeSegment(projectName) + "/graphql" + getQuery(getConfig(), parameters);
		return prepareRequest(POST, path, GraphQLResponse.class, request);
	}

	@Override
	public MeshRequest<JobListResponse> findJobs(ParameterProvider... parameters) {
		return prepareRequest(GET, "/admin/jobs" + getQuery(getConfig(), parameters), JobListResponse.class);
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
	public MeshRequest<JobResponse> processJob(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(POST, "/admin/jobs/" + uuid + "/process", JobResponse.class);
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
		return prepareRequest(GET, "/admin/plugins" + getQuery(getConfig(), parameters), PluginListResponse.class);
	}

	@Override
	public MeshRequest<PluginResponse> findPlugin(String id) {
		Objects.requireNonNull(id, "id must not be null");
		return prepareRequest(GET, "/admin/plugins/" + id, PluginResponse.class);
	}

	@Override
	public MeshRequest<MeshBinaryResponse> debugInfo(String... includes) {
		String includeString = Stream.of(includes)
			.collect(Collectors.joining(","));
		if (!includeString.isEmpty()) {
			includeString = "?include=" + includeString;
		}
		return prepareRequest(GET, "/admin/debuginfo" + includeString, MeshBinaryResponse.class);
	}

	@Override
	public MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster() {
		return prepareRequest(GET, "/admin/coordinator/master", CoordinatorMasterResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> setCoordinationMaster() {
		return prepareRequest(POST, "/admin/coordinator/master", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<GenericMessageResponse> clearCache() {
		return prepareRequest(DELETE, "/admin/cache", GenericMessageResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> ready() {
		return prepareRequest(GET, "/health/ready", EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> live() {
		return prepareRequest(GET, "/health/live", EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> writable() {
		return prepareRequest(GET, "/health/writable", EmptyResponse.class);
	}

	@Override
	public MeshRequest<LocalConfigModel> loadLocalConfig() {
		return prepareRequest(GET, "/admin/config", LocalConfigModel.class);
	}

	@Override
	public MeshRequest<LocalConfigModel> updateLocalConfig(LocalConfigModel localConfigModel) {
		return prepareRequest(POST, "/admin/config", LocalConfigModel.class, localConfigModel);
	}

	@Override
	public MeshRequest<JsonObject> get(String path) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(GET, path, JsonObject.class);
	}

	@Override
	public <R> MeshRequest<R> get(String path, Class<R> responseClass) {
		return prepareRequest(GET, path, responseClass);
	}

	@Override
	public MeshRequest<JsonObject> put(String path) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(PUT, path, JsonObject.class);
	}

	@Override
	public MeshRequest<JsonObject> put(String path, JsonObject body) {
		Objects.requireNonNull(path, "path must not be null");
		Objects.requireNonNull(body, "body must not be null");
		return handleRequest(PUT, path, JsonObject.class, isMinifyJson() ? body.encode() : body.encodePrettily());
	}

	@Override
	public <R> MeshRequest<R> put(String path, Class<R> responseClass) {
		return prepareRequest(PUT, path, responseClass);
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass) {
		Objects.requireNonNull(path, "path must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return prepareRequest(PUT, path, responseClass, request);
	}

	@Override
	public MeshRequest<JsonObject> post(String path) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(POST, path, JsonObject.class);
	}

	@Override
	public MeshRequest<JsonObject> post(String path, JsonObject body) {
		Objects.requireNonNull(path, "path must not be null");
		Objects.requireNonNull(body, "body must not be null");
		return handleRequest(POST, path, JsonObject.class, isMinifyJson() ? body.encode() : body.encodePrettily());
	}

	@Override
	public <R> MeshRequest<R> post(String path, Class<R> responseClass) {
		return prepareRequest(POST, path, responseClass);
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> responseClass) {
		Objects.requireNonNull(request, "The request must not be null");
		return prepareRequest(POST, path, responseClass, request);
	}

	@Override
	public MeshRequest<JsonObject> delete(String path) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(DELETE, path, JsonObject.class);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteEmpty(String path) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(DELETE, path, EmptyResponse.class);
	}

	@Override
	public <R> MeshRequest<R> delete(String path, Class<R> responseClass) {
		Objects.requireNonNull(path, "path must not be null");
		return prepareRequest(DELETE, path, responseClass);
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String path, ImageManipulationRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(fieldName, "fieldName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return upsertWebrootFieldImageVariants(projectName, fieldName, path.split("/"), request, parameters);
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String[] pathSegments, ImageManipulationRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(fieldName, "fieldName must not be null");
		Objects.requireNonNull(pathSegments, "pathSegments must not be null");

		String path = Arrays.stream(pathSegments)
			.filter(segment -> segment != null && !segment.isEmpty())
			.map(URIUtils::encodeSegment)
			.collect(Collectors.joining("/", "/", ""));

		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/webrootfield/" + fieldName + path + getQuery(getConfig(), parameters), ImageVariantsResponse.class, request);
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String path,
			ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(fieldName, "fieldName must not be null");
		Objects.requireNonNull(path, "path must not be null");
		if (!path.startsWith("/")) {
			throw new RuntimeException("The path {" + path + "} must start with a slash");
		}
		return webrootField(projectName, fieldName, path.split("/"), parameters);
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName,
			String[] pathSegments, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(fieldName, "fieldName must not be null");
		Objects.requireNonNull(pathSegments, "pathSegments must not be null");

		String path = Arrays.stream(pathSegments)
			.filter(segment -> segment != null && !segment.isEmpty())
			.map(URIUtils::encodeSegment)
			.collect(Collectors.joining("/", "/", ""));

		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/webrootfield/" + fieldName + path + getQuery(getConfig(), parameters), MeshWebrootFieldResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getBranchRolePermissions(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/branches/" + uuid + "/rolePermissions",
				ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/branches/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/branches/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getGroupRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/groups/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantGroupRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/groups/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeGroupRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/groups/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getMicroschemaRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/microschemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantMicroschemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/microschemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeMicroschemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/microschemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getNodeRolePermissions(String projectName, String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/rolePermissions",
				ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getProjectRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/projects/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantProjectRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/projects/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeProjectRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/projects/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getRoleRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/roles/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantRoleRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/roles/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeRoleRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/roles/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getSchemaRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/schemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantSchemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/schemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeSchemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/schemas/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagFamilyRolePermissions(String projectName, String tagFamilyUuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/rolePermissions",
				ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagFamilyRolePermissions(String projectName, String tagFamilyUuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagFamilyRolePermissions(String projectName, String tagFamilyUuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid + "/rolePermissions",
				ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagRolePermissions(String projectName, String tagFamilyUuid, String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagRolePermissions(String projectName, String tagFamilyUuid, String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(tagFamilyUuid, "tagFamilyUuid must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/tagFamilies/" + tagFamilyUuid + "/tags/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getUserRolePermissions(String uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return prepareRequest(GET, "/users/" + uuid + "/rolePermissions", ObjectPermissionResponse.class);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantUserRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(POST, "/users/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeUserRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(request, "objectPermissionRequest must not be null");
		return prepareRequest(DELETE, "/users/" + uuid + "/rolePermissions", ObjectPermissionResponse.class, request);
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertNodeBinaryFieldImageVariants(String projectName, String nodeUuid, String fieldKey, ImageManipulationRequest request, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fieldKey, "fieldKey must not be null");
		return prepareRequest(POST, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + "/variants" + getQuery(getConfig(), parameters), ImageVariantsResponse.class, request);
	}

	@Override
	public MeshRequest<EmptyResponse> clearNodeBinaryFieldImageVariants(String projectName, String nodeUuid, String fieldKey, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fieldKey, "fieldKey must not be null");
		return prepareRequest(DELETE, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + "/variants" + getQuery(getConfig(), parameters), EmptyResponse.class);
	}

	@Override
	public MeshRequest<ImageVariantsResponse> getNodeBinaryFieldImageVariants(String projectName, String nodeUuid, String fieldKey, ParameterProvider... parameters) {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(nodeUuid, "nodeUuid must not be null");
		Objects.requireNonNull(fieldKey, "fieldKey must not be null");
		return prepareRequest(GET, "/" + encodeSegment(projectName) + "/nodes/" + nodeUuid + "/binary/" + fieldKey + "/variants" + getQuery(getConfig(), parameters), ImageVariantsResponse.class);
	}
}

package com.gentics.mesh.test.openapi;

import java.io.InputStream;
import java.util.Arrays;

import org.openapitools.client.ApiClient;
import org.openapitools.client.model.LoginRequest;

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
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.openapi.Format;
import com.gentics.mesh.core.rest.openapi.Version;
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
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.parameter.BranchParameters;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.JobParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.RolePermissionParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.rest.JWTAuthentication;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.MeshWebsocket;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;

@SuppressWarnings({"unchecked","rawtypes"})
public class OpenAPIMeshRestClient implements MeshRestClient {

	private final ApiClient apiClient;
	private final UpgradedDefaultApi api;
	private final MeshRestClientConfig config;
	private final JWTAuthentication authentication = new JWTAuthentication();

	private static final <T> T findParameter(String key, ParameterProvider... parameters) {
		return (T) Arrays.stream(parameters).map(p -> p.getParameter(key)).findAny().orElse(null);
	}

	public OpenAPIMeshRestClient(MeshRestClientConfig config, OkHttpClient okHttp) {
		this.apiClient = new ApiClient(okHttp).setBasePath("%s://%s:%d".formatted((config.isSsl() ? "https" : "http"), config.getHost(), config.getPort()));
		this.api = new UpgradedDefaultApi(this.apiClient);
		this.config = config;
	}

	@Override
	public MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidGetWithHttpInfo(
				uuid, projectName, 
				findParameter(GenericParameters.ETAG_PARAM_KEY, parameters), 
				findParameter(RolePermissionParameters.ROLE_PERMISSION_QUERY_PARAM_KEY, parameters), 
				findParameter(NodeParameters.LANGUAGES_QUERY_PARAM_KEY, parameters), 
				findParameter(GenericParameters.FIELDS_PARAM_KEY, parameters), 
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters), 
				findParameter(NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesPostWithHttpInfo(projectName, 
				org.openapitools.client.model.NodeCreateRequest.fromJson(JsonUtil.toJson(nodeCreateRequest))));
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPostWithHttpInfo(uuid, projectName, new JsonObject(nodeCreateRequest.toJson())));
	}

	@Override
	public MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsertRequest,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPostWithHttpInfo(uuid, projectName, new JsonObject(nodeUpsertRequest.toJson())));
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPutWithHttpInfo(uuid, projectName, 
				org.openapitools.client.model.NodeUpdateRequest.fromJson(JsonUtil.toJson(nodeUpdateRequest))));
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidDeleteWithHttpInfo(uuid, projectName,
				findParameter(BranchParameters.BRANCH_QUERY_PARAM_KEY, parameters),
				findParameter(DeleteParameters.RECURSIVE_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, String languageTag,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidLanguagesLanguageDeleteWithHttpInfo(
				languageTag, uuid, projectName,
				findParameter(BranchParameters.BRANCH_QUERY_PARAM_KEY, parameters),
				findParameter(DeleteParameters.RECURSIVE_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesGetWithHttpInfo(projectName, 
				findParameter(RolePermissionParameters.ROLE_PERMISSION_QUERY_PARAM_KEY, parameters), 
				findParameter(PagingParameters.PER_PAGE_PARAMETER_KEY, parameters), 
				findParameter(NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY, parameters), 
				findParameter(GenericParameters.ETAG_PARAM_KEY, parameters), 
				findParameter(PagingParameters.SORT_BY_PARAMETER_KEY, parameters), 
				findParameter(PagingParameters.PAGE_PARAMETER_KEY, parameters), 
				findParameter(NodeParameters.LANGUAGES_QUERY_PARAM_KEY, parameters), 
				findParameter(GenericParameters.FIELDS_PARAM_KEY, parameters), 
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters), 
				findParameter(PagingParameters.SORT_ORDER_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidChildrenGetWithHttpInfo(
				parentNodeUuid, projectName, 
				findParameter(PagingParameters.PER_PAGE_PARAMETER_KEY, parameters), 
				findParameter(NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY, parameters), 
				findParameter(PagingParameters.SORT_BY_PARAMETER_KEY, parameters), 
				findParameter(GenericParameters.ETAG_PARAM_KEY, parameters), 
				findParameter(PagingParameters.PAGE_PARAMETER_KEY, parameters), 
				findParameter(NodeParameters.LANGUAGES_QUERY_PARAM_KEY, parameters), 
				findParameter(GenericParameters.FIELDS_PARAM_KEY, parameters), 
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters), 
				findParameter(PagingParameters.SORT_ORDER_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid,
			ParameterProvider... parameters) {
		
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectTagFamiliesTagFamilyUuidTagsTagUuidGetWithHttpInfo(
				tagFamilyUuid, tagUuid, projectName, 
				findParameter(GenericParameters.FIELDS_PARAM_KEY, parameters), 
				findParameter(GenericParameters.ETAG_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidTagsTagUuidPostWithHttpInfo(
				tagUuid, nodeUuid, projectName,
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidTagsTagUuidDeleteWithHttpInfo(tagUuid, nodeUuid, projectName));
	}

	@Override
	public MeshRequest<EmptyResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidMoveToToUuidPostWithHttpInfo(
				targetFolderUuid, nodeUuid, projectName,
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidTagsGetWithHttpInfo(nodeUuid, projectName, 
				findParameter(GenericParameters.FIELDS_PARAM_KEY, parameters), 
				findParameter(VersioningParameters.VERSION_QUERY_PARAM_KEY, parameters), 
				findParameter(GenericParameters.ETAG_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid,
			TagListUpdateRequest request, ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidTagsPostWithHttpInfo(
				nodeUuid, projectName, 
				org.openapitools.client.model.TagListUpdateRequest.fromJson(JsonUtil.toJson(request))));
	}

	@Override
	public MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPublishedGetWithHttpInfo(nodeUuid, projectName));
	}

	@Override
	public MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid,
			String languageTag, ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidLanguagesLanguagePublishedGetWithHttpInfo(languageTag, nodeUuid, projectName));
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPublishedPostWithHttpInfo(nodeUuid, projectName,
				findParameter(PublishParameters.RECURSIVE_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidLanguagesLanguagePublishedPostWithHttpInfo(languageTag, nodeUuid, projectName));
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidPublishedDeleteWithHttpInfo(nodeUuid, projectName,
				findParameter(PublishParameters.RECURSIVE_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidLanguagesLanguagePublishedDeleteWithHttpInfo(languageTag, nodeUuid, projectName));
	}

	@Override
	public MeshRequest<NodeVersionsResponse> listNodeVersions(String projectName, String nodeUuid,
			ParameterProvider... parameters) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidVersionsGetWithHttpInfo(nodeUuid, projectName, 
				findParameter(NodeParameters.LANGUAGES_QUERY_PARAM_KEY, parameters), 
				findParameter(NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY, parameters)));
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getNodeRolePermissions(String projectName, String uuid) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2ProjectNodesNodeUuidRolePermissionsGetWithHttpInfo(uuid, projectName));
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid,
			TagUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, String uuid,
			TagCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid, ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid, ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(ProjectCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(String uuid, ProjectCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteProject(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> purgeProject(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getProjectRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantProjectRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeProjectRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTagFamily(String projectName, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid,
			TagFamilyUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, String tagFamilyUuid,
			TagFamilyCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagFamilyRolePermissions(String projectName, String tagFamilyUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagFamilyRolePermissions(String projectName, String tagFamilyUuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagFamilyRolePermissions(String projectName,
			String tagFamilyUuid, ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String[] pathSegments,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String path, NodeUpdateRequest nodeUpdateRequest,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String[] pathSegments,
			NodeUpdateRequest nodeUpdateRequest, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String path, NodeCreateRequest nodeCreateRequest,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String[] pathSegments,
			NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(String uuid, SchemaCreateRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, SchemaModel request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteSchema(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> unassignSchemaFromProject(String projectName, String schemaUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaResponse> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getSchemaRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantSchemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeSchemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(GroupCreateRequest createRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(String uuid, GroupCreateRequest createRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteGroup(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getGroupRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantGroupRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeGroupRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserResponse> createUser(UserCreateRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteUser(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserResetTokenResponse> getUserResetToken(String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserAPITokenResponse> issueAPIToken(String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invalidateAPIToken(String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getUserRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantUserRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeUserRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleResponse> createRole(RoleCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleResponse> createRole(String uuid, RoleCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteRole(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement,
			RolePermissionRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getRoleRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantRoleRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeRoleRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Single<GenericMessageResponse> login() {
		return Single.fromCallable(() -> api.apiV2AuthLoginPost(new LoginRequest()
				.username(authentication.getUsername())
				.password(authentication.getPassword())
				.newPassword(authentication.getNewPassword())))
			.map(tokenResponse -> {
				authentication.setToken(tokenResponse.getToken());
				apiClient.setBearerToken(tokenResponse.getToken());
				return new GenericMessageResponse("OK");
			});
	}

	@Override
	public Single<GenericMessageResponse> logout() {
		return Single.fromCallable(() -> api.apiV2AuthLogoutGet())
			.map(response -> {
				authentication.setToken(null);
				return new GenericMessageResponse("OK");
			});
	}

	@Override
	public MeshRequest<UserResponse> me(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchUsersRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchGroupsRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchRolesRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchProjectsRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String projectName, String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String projectName, String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchSchemasRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchMicroschemasRaw(String json) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexClear(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexSync(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SearchStatusResponse> searchStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshStatusResponse> meshStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ClusterStatusResponse> clusterStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup(BackupParameters parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeExport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeRestore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeImport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> checkConsistency(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> repairConsistency(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshBinaryResponse> debugInfo(String... include) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> setCoordinationMaster() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> clearCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<PluginResponse> deployPlugin(PluginDeploymentRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<PluginListResponse> findPlugins(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<PluginResponse> findPlugin(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> undeployPlugin(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(String uuid, MicroschemaCreateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteMicroschema(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, MicroschemaModel request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getMicroschemaRolePermissions(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantMicroschemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeMicroschemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag,
			String nodeVersion, String fieldKey, InputStream fileData, long fileSize, String fileName,
			String contentType, boolean publish, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag,
			String fieldKey, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag,
			String fieldKey, long from, long to, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag,
			String version, String fieldKey, ImageManipulationParameters imageManipulationParameter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryFieldCheckStatus(String projectName, String nodeUuid,
			String languageTag, String nodeVersion, String fieldKey, String secret, String branchUuid,
			BinaryCheckStatus status, String reason) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ImageManipulationRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> clearNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ImageVariantsResponse> getNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey,
			S3BinaryUploadRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid,
			String fieldKey, S3BinaryMetadataRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateSchema(SchemaModel schema) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateMicroschema(MicroschemaModel microschemaModel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshWebsocket eventbus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, String uuid,
			BranchCreateRequest branchCreateRequest, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> updateBranch(String projectName, String branchUuid,
			BranchUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> getBranchSchemaVersions(String projectName, String branchUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
			BranchInfoSchemaList schemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
			SchemaReference... schemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> getBranchMicroschemaVersions(String projectName, String branchUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
			BranchInfoMicroschemaList microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
			MicroschemaReference... microschemaVersionReferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchSchemas(String projectName, String branchUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchMicroschemas(String projectName, String branchUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> setLatestBranch(String projectName, String branchUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> addTagToBranch(String projectName, String branchUuid, String tagUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromBranch(String projectName, String branchUuid, String tagUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForBranch(String projectName, String branchUuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForBranch(String projectName, String branchUuid,
			TagListUpdateRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getBranchRolePermissions(String projectName, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshServerInfoModel> getApiInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<String> getRAML() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<String> getOpenAPI(Format format, Version version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JobListResponse> findJobs(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminJobsGetWithHttpInfo(
				findParameter(JobParameters.FROM_VERSION_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.MICROSCHEMA_NAME_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.MICROSCHEMA_UUID_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.BRANCH_NAME_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.BRANCH_UUID_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.TYPE_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.SCHEMA_NAME_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.STATUS_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.SCHEMA_UUID_PARAMETER_KEY, parameters), 
				findParameter(JobParameters.TO_VERSION_PARAMETER_KEY, parameters)));
	}

	@Override
	public MeshRequest<JobResponse> findJobByUuid(String uuid) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminJobsJobUuidGetWithHttpInfo(uuid));
	}

	@Override
	public MeshRequest<EmptyResponse> deleteJob(String uuid) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminJobsJobUuidDeleteWithHttpInfo(uuid));
	}

	@Override
	public MeshRequest<EmptyResponse> resetJob(String uuid) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminJobsJobUuidErrorDeleteWithHttpInfo(uuid));
	}

	@Override
	public MeshRequest<JobResponse> processJob(String uuid) {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminJobsJobUuidProcessPostWithHttpInfo(uuid));
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeJobProcessing() {
		return new OpenAPIMeshRequestImpl(() -> api.apiV2AdminProcessJobsPostWithHttpInfo());
	}

	@Override
	public MeshRequest<JsonObject> get(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> get(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> post(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> post(String path, JsonObject body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> post(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> put(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> put(String path, JsonObject body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> put(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> delete(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteEmpty(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> delete(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> ready() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> live() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> writable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LocalConfigModel> loadLocalConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LocalConfigModel> updateLocalConfig(LocalConfigModel localConfigModel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String path,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName,
			String[] pathSegments, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String path, ImageManipulationRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String[] pathSegments, ImageManipulationRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByUuid(String uuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByTag(String tag, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageListResponse> findLanguages(ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByUuid(String projectName, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageResponse> findLanguageByTag(String projectName, String tag,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<LanguageListResponse> findLanguages(String projectName, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProjectByUuid(String projectName, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProjectByTag(String projectName, String tag,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProjectByUuid(String projectName, String uuid,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProjectByTag(String projectName, String tag,
			ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient setLogin(String username, String password) {
		authentication.setLogin(username, password);
		return this;
	}

	@Override
	public MeshRestClient setLogin(String username, String password, String newPassword) {
		authentication.setLogin(username, password, newPassword);
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public MeshRestClient setAPIKey(String apiKey) {
		authentication.setToken(apiKey);
		return this;
	}

	@Override
	public String getAPIKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient disableAnonymousAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient enableAnonymousAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient setAuthenticationProvider(JWTAuthentication authentication) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JWTAuthentication getAuthentication() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClientConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

}

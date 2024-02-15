package com.gentics.mesh.plugin.comment.testclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
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
import com.gentics.mesh.core.rest.schema.SchemaModel;
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
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.ParameterProvider;
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

import org.apache.commons.lang.NotImplementedException;

import java.io.InputStream;

/**
 * A client that throws an Exception for every method. Extend this class and override every method you wish to mock.
 */
public abstract class AbstractTestClient implements MeshRestClient {

	@Override
	public MeshRestClient setLogin(String username, String password) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClient setLogin(String username, String password, String newPassword) {
		throw new NotImplementedException();
	}

	@Override
	public void close() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClient setAPIKey(String apiKey) {
		throw new NotImplementedException();
	}

	@Override
	public String getAPIKey() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClient disableAnonymousAccess() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClient enableAnonymousAccess() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClient setAuthenticationProvider(JWTAuthentication authentication) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshStatusResponse> meshStatus() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ClusterStatusResponse> clusterStatus() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup(BackupParameters parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeExport() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeRestore() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeImport() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> checkConsistency(ParameterProvider... parameterProviders) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> repairConsistency(ParameterProvider... parameterProviders) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshServerInfoModel> getApiInfo() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<String> getRAML() {
		throw new NotImplementedException();
	}

	@Override
	public Single<GenericMessageResponse> login() {
		throw new NotImplementedException();
	}

	@Override
	public Single<GenericMessageResponse> logout() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResponse> me(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GraphQLResponse> graphqlQuery(String projectName, String query, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(GroupCreateRequest createRequest) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(String uuid, GroupCreateRequest createRequest) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteGroup(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> addUserToGroup(String groupUuid, String userUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getGroupRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantGroupRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeGroupRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JobListResponse> findJobs(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JobResponse> findJobByUuid(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteJob(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> resetJob(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JobResponse> processJob(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeJobProcessing() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(String uuid, MicroschemaCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteMicroschema(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, MicroschemaModel request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getMicroschemaRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantMicroschemaRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeMicroschemaRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NavigationResponse> navroot(String projectName, String path, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NavigationResponse> loadNavigation(String projectName, String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version,
		String fieldKey, ImageManipulationParameters imageManipulationParameter) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsertRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeVersionsResponse> listNodeVersions(String projectName, String nodeUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getNodeRolePermissions(String projectName, String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantNodeRolePermissions(String projectName, String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeNodeRolePermissions(String projectName, String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(ProjectCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> createProject(String uuid, ProjectCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteProject(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> purgeProject(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getProjectRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantProjectRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeProjectRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, String uuid, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> updateBranch(String projectName, String branchUuid, BranchUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> getBranchSchemaVersions(String projectName, String branchUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
		BranchInfoSchemaList schemaVersionReferences) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid,
		SchemaReference... schemaVersionReferences) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> getBranchMicroschemaVersions(String projectName, String branchUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
		BranchInfoMicroschemaList microschemaVersionReferences) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
		MicroschemaReference... microschemaVersionReferences) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameter) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleResponse> createRole(RoleCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleResponse> createRole(String uuid, RoleCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteRole(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryFieldCheckStatus(String projectName, String nodeUuid, String languageTag, String nodeVersion, String fieldKey, String secret, String branchUuid, BinaryCheckStatus status, String reason) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getRoleRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantRoleRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeRoleRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest schemaCreateRequest, ParameterProvider... parameterProviders) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(String s, SchemaCreateRequest schemaCreateRequest, ParameterProvider... parameterProviders) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, SchemaModel request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteSchema(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> unassignSchemaFromProject(String projectName, String schemaUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaResponse> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getSchemaRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantSchemaRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeSchemaRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String projectName, String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchUsersRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchGroupsRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchRolesRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchProjectsRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String projectName, String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String projectName, String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchSchemasRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectNode> searchMicroschemasRaw(String json) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexClear(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexSync(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SearchStatusResponse> searchStatus() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, String uuid, TagCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagRolePermissions(String projectName, String tagFamilyUuid, String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagRolePermissions(String projectName, String tagFamilyUuid, String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagRolePermissions(String projectName, String tagFamilyUuid, String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyResponse> findTagFamilyByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, PagingParameters pagingInfo) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, TagFamilyCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTagFamily(String projectName, String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyResponse> updateTagFamily(String projectName, String tagFamilyUuid, TagFamilyUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, String tagFamilyUuid, TagFamilyCreateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagFamilyListResponse> findTagFamilies(String projectName, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagFamilyRolePermissions(String projectName, String tagFamilyUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagFamilyRolePermissions(String projectName, String tagFamilyUuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagFamilyRolePermissions(String projectName, String tagFamilyUuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResponse> createUser(UserCreateRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteUser(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserResetTokenResponse> getUserResetToken(String userUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<UserAPITokenResponse> issueAPIToken(String userUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> invalidateAPIToken(String userUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getUserRolePermissions(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantUserRolePermissions(String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeUserRolePermissions(String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateSchema(SchemaModel schema) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateMicroschema(MicroschemaModel microschema) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String path,
			ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName,
			String[] pathSegments, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public JWTAuthentication getAuthentication() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRestClientConfig getConfig() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PluginResponse> deployPlugin(PluginDeploymentRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PluginListResponse> findPlugins(ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<PluginResponse> findPlugin(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> undeployPlugin(String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchSchemas(String projectName, String branchUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> migrateBranchMicroschemas(String projectName, String branchUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> setLatestBranch(String projectName, String branchUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<BranchResponse> addTagToBranch(String projectName, String branchUuid, String tagUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromBranch(String projectName, String branchUuid, String tagUuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForBranch(String projectName, String branchUuid, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForBranch(String projectName, String branchUuid, TagListUpdateRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getBranchRolePermissions(String projectName, String uuid) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantBranchRolePermissions(String projectName, String uuid, ObjectPermissionGrantRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeBranchRolePermissions(String projectName, String uuid, ObjectPermissionRevokeRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String path, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String[] pathSegments, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String path, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String[] pathSegments, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshWebsocket eventbus() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String nodeVersion, String fieldKey, InputStream fileData, long fileSize, String fileName, String contentType, boolean publish, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey, long from,
		long to, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> get(String path) {
		throw new NotImplementedException();
	}

	@Override
	public <R> MeshRequest<R> get(String path, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> post(String path) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> post(String path, JsonObject body) {
		throw new NotImplementedException();
	}

	@Override
	public <R> MeshRequest<R> post(String path, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> put(String path) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> put(String path, JsonObject body) {
		throw new NotImplementedException();
	}

	@Override
	public <R> MeshRequest<R> put(String path, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<JsonObject> delete(String path) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> deleteEmpty(String path) {
		throw new NotImplementedException();
	}

	@Override
	public <R> MeshRequest<R> delete(String path, Class<R> responseClass) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<MeshBinaryResponse> debugInfo(String... include) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> setCoordinationMaster() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<CoordinatorConfig> loadCoordinationConfig() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<CoordinatorConfig> updateCoordinationConfig(CoordinatorConfig coordinatorConfig) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> live() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> ready() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<LocalConfigModel> loadLocalConfig() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<LocalConfigModel> updateLocalConfig(LocalConfigModel arg0) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ClusterConfigResponse> loadClusterConfig() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<ClusterConfigResponse> updateClusterConfig(ClusterConfigRequest request) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<GenericMessageResponse> clearCache() {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey,
			S3BinaryUploadRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid,
			String fieldKey, S3BinaryMetadataRequest request, ParameterProvider... parameters) {
		throw new NotImplementedException();
	}

	@Override
	public MeshRequest<EmptyResponse> writable() {
		throw new NotImplementedException();
	}
}

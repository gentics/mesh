package com.gentics.mesh.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.plugin.PluginHandler;
import com.gentics.mesh.core.endpoint.auth.AuthenticationRestHandler;
import com.gentics.mesh.core.endpoint.branch.BranchCrudHandler;
import com.gentics.mesh.core.endpoint.group.GroupCrudHandler;
import com.gentics.mesh.core.endpoint.microschema.MicroschemaCrudHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.endpoint.node.BinaryVariantsHandler;
import com.gentics.mesh.core.endpoint.node.NodeCrudHandler;
import com.gentics.mesh.core.endpoint.node.S3BinaryMetadataExtractionHandlerImpl;
import com.gentics.mesh.core.endpoint.node.S3BinaryUploadHandlerImpl;
import com.gentics.mesh.core.endpoint.project.ProjectCrudHandler;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandlerImpl;
import com.gentics.mesh.core.endpoint.schema.SchemaCrudHandler;
import com.gentics.mesh.core.endpoint.tag.TagCrudHandler;
import com.gentics.mesh.core.endpoint.tagfamily.TagFamilyCrudHandler;
import com.gentics.mesh.core.endpoint.user.UserCrudHandler;
import com.gentics.mesh.core.endpoint.utility.UtilityHandler;
import com.gentics.mesh.core.endpoint.webroot.WebRootHandler;
import com.gentics.mesh.core.endpoint.webrootfield.WebRootFieldHandler;
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
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.MeshWebsocket;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.search.index.AdminIndexHandler;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

/**
 * Local client implementation. This client will invoke endpoint handlers instead of sending http rest requests. Please note that is implementation is not very
 * well tested and may lack a lot of endpoint implementations.
 */
@Singleton
public class MeshLocalClientImpl implements MeshLocalClient {

	public MeshAuthUser user;

	protected final BinaryVariantsHandler binaryVariantsHandler;

	protected final UserCrudHandler userCrudHandler;

	protected final RoleCrudHandlerImpl roleCrudHandler;

	protected final GroupCrudHandler groupCrudHandler;

	protected final SchemaCrudHandler schemaCrudHandler;

	protected final MicroschemaCrudHandler microschemaCrudHandler;

	protected final TagCrudHandler tagCrudHandler;

	protected final TagFamilyCrudHandler tagFamilyCrudHandler;

	protected final ProjectCrudHandler projectCrudHandler;

	protected final NodeCrudHandler nodeCrudHandler;

	protected final BinaryUploadHandlerImpl fieldAPIHandler;

	protected final S3BinaryUploadHandlerImpl s3fieldAPIHandler;

	protected final S3BinaryMetadataExtractionHandlerImpl s3BinaryMetadataExtractionHandler;

	protected final WebRootHandler webrootHandler;

	protected final WebRootFieldHandler webrootFieldHandler;

	protected final AdminHandler adminHandler;

	protected final AdminIndexHandler adminIndexHandler;

	protected final AuthenticationRestHandler authRestHandler;

	protected final UtilityHandler utilityHandler;

	protected final BranchCrudHandler branchCrudHandler;

	protected final PluginHandler pluginHandler;

	protected final Vertx vertx;

	protected final BootstrapInitializer boot;

	@Inject
	public MeshLocalClientImpl(UserCrudHandler userCrudHandler, RoleCrudHandlerImpl roleCrudHandler,
			GroupCrudHandler groupCrudHandler, SchemaCrudHandler schemaCrudHandler,
			MicroschemaCrudHandler microschemaCrudHandler, TagCrudHandler tagCrudHandler,
			TagFamilyCrudHandler tagFamilyCrudHandler, ProjectCrudHandler projectCrudHandler,
			NodeCrudHandler nodeCrudHandler, BinaryUploadHandlerImpl fieldAPIHandler,
			S3BinaryUploadHandlerImpl s3fieldAPIHandler,
			S3BinaryMetadataExtractionHandlerImpl s3BinaryMetadataExtractionHandler, WebRootHandler webrootHandler,
			WebRootFieldHandler webrootFieldHandler, AdminHandler adminHandler, AdminIndexHandler adminIndexHandler,
			AuthenticationRestHandler authRestHandler, UtilityHandler utilityHandler,
			BranchCrudHandler branchCrudHandler, BinaryVariantsHandler binaryVariantsHandler, PluginHandler pluginHandler, Vertx vertx, BootstrapInitializer boot) {
		this.binaryVariantsHandler = binaryVariantsHandler;
		this.userCrudHandler = userCrudHandler;
		this.roleCrudHandler = roleCrudHandler;
		this.groupCrudHandler = groupCrudHandler;
		this.schemaCrudHandler = schemaCrudHandler;
		this.microschemaCrudHandler = microschemaCrudHandler;
		this.tagCrudHandler = tagCrudHandler;
		this.tagFamilyCrudHandler = tagFamilyCrudHandler;
		this.projectCrudHandler = projectCrudHandler;
		this.nodeCrudHandler = nodeCrudHandler;
		this.fieldAPIHandler = fieldAPIHandler;
		this.s3fieldAPIHandler = s3fieldAPIHandler;
		this.s3BinaryMetadataExtractionHandler = s3BinaryMetadataExtractionHandler;
		this.webrootHandler = webrootHandler;
		this.webrootFieldHandler = webrootFieldHandler;
		this.adminHandler = adminHandler;
		this.adminIndexHandler = adminIndexHandler;
		this.authRestHandler = authRestHandler;
		this.utilityHandler = utilityHandler;
		this.branchCrudHandler = branchCrudHandler;
		this.pluginHandler = pluginHandler;
		this.vertx = vertx;
		this.boot = boot;
	}

	private Map<String, HibProject> projects = new HashMap<>();

	@Override
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
	public MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsetRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(nodeUpsetRequest);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleUpdate(ac, uuid);
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
	public MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(nodeCreateRequest);
		ac.getVersioningParameters().setVersion("draft");
		nodeCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setPayloadObject(nodeUpdateRequest);
		ac.setProject(projectName);
		nodeCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
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
	public MeshRequest<EmptyResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleRemoveTag(ac, nodeUuid, tagUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());

	}

	@Override
	public MeshRequest<EmptyResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleMove(ac, nodeUuid, targetFolderUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeVersionsResponse> listNodeVersions(String projectName, String nodeUuid, ParameterProvider... parameters) {
		return null;
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
		ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, TagCreateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> findTagByUuid(String projectName, String tagFamilyUuid, String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class, parameters);
		ac.setProject(projectName);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> updateTag(String projectName, String tagFamilyUuid, String uuid, TagUpdateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagResponse> createTag(String projectName, String tagFamilyUuid, String uuid, TagCreateRequest request) {
		LocalActionContextImpl<TagResponse> ac = createContext(TagResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> findTags(String projectName, String tagFamilyUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
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
		// TODO add implementation
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		// TODO add implementation
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
	public MeshRequest<ProjectResponse> createProject(String uuid, ProjectCreateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		ac.setPayloadObject(request);
		projectCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request) {
		LocalActionContextImpl<ProjectResponse> ac = createContext(ProjectResponse.class);
		projectCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteProject(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
		projectCrudHandler.handleDelete(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> purgeProject(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		projectCrudHandler.handlePurge(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<SchemaResponse> ac = createContext(SchemaResponse.class);
		ac.setProject(projectName);
		schemaCrudHandler.handleAddSchemaToProject(ac, schemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> unassignSchemaFromProject(String projectName, String schemaUuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<MicroschemaResponse> assignMicroschemaToProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<MicroschemaResponse> ac = createContext(MicroschemaResponse.class);
		microschemaCrudHandler.handleAddMicroschemaToProject(ac, microschemaUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> unassignMicroschemaFromProject(String projectName, String microschemaUuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<EmptyResponse> deleteTagFamily(String projectName, String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<TagFamilyResponse> createTagFamily(String projectName, String tagFamilyUuid, TagFamilyCreateRequest request) {
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
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName, String path,
			ParameterProvider... parameters) {
		LocalActionContextImpl<MeshWebrootFieldResponse> ac = createContext(MeshWebrootFieldResponse.class, parameters);
		ac.setProject(projectName);
		//webrootFieldHandler.handleGetPathField(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MeshWebrootFieldResponse> webrootField(String projectName, String fieldName,
			String[] pathSegments, ParameterProvider... parameters) {
		LocalActionContextImpl<MeshWebrootFieldResponse> ac = createContext(MeshWebrootFieldResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String path, ImageManipulationRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<ImageVariantsResponse> ac = createContext(ImageVariantsResponse.class, parameters);
		ac.setProject(projectName);
		//webrootFieldHandler.handlePostPathField(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertWebrootFieldImageVariants(String projectName, String fieldName,
			String[] pathSegments, ImageManipulationRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<ImageVariantsResponse> ac = createContext(ImageVariantsResponse.class, parameters);
		ac.setProject(projectName);
		//webrootFieldHandler.handlePostPathField(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		LocalActionContextImpl<MeshWebrootResponse> ac = createContext(MeshWebrootResponse.class, parameters);
		ac.setProject(projectName);
		// webrootHandler.handleGetPath(rc);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String[] pathSegments, ParameterProvider... parameters) {
		LocalActionContextImpl<MeshWebrootResponse> ac = createContext(MeshWebrootResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String path, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> webrootUpdate(String projectName, String[] pathSegments, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String path, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> webrootCreate(String projectName, String[] pathSegments, NodeCreateRequest nodeCreateRequest,
		ParameterProvider... parameters) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaResponse> ac = createContext(SchemaResponse.class, parameters);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(String uuid, SchemaCreateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaResponse> ac = createContext(SchemaResponse.class, parameters);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaResponse> ac = createContext(SchemaResponse.class, parameters);
		schemaCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffSchema(String uuid, SchemaModel request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleDiff(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteSchema(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<GroupResponse> createGroup(GroupCreateRequest createRequest) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(createRequest);
		groupCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupResponse> createGroup(String uuid, GroupCreateRequest createRequest) {
		LocalActionContextImpl<GroupResponse> ac = createContext(GroupResponse.class);
		ac.setPayloadObject(createRequest);
		groupCrudHandler.handleUpdate(ac, uuid);
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
	public MeshRequest<EmptyResponse> deleteGroup(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<EmptyResponse> removeUserFromGroup(String groupUuid, String userUuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<EmptyResponse> removeRoleFromGroup(String groupUuid, String roleUuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		ac.setPayloadObject(request);
		userCrudHandler.handleUpdate(ac, uuid);
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
	public MeshRequest<EmptyResponse> deleteUser(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
	public MeshRequest<RoleResponse> createRole(String uuid, RoleCreateRequest request) {
		LocalActionContextImpl<RoleResponse> ac = createContext(RoleResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteRole(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
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
		return Single.error(new Exception("Not implemented"));
	}

	@Override
	public Single<GenericMessageResponse> logout() {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		ac.logout();
		return null;
	}

	@Override
	public MeshRequest<UserResponse> me(ParameterProvider... parameters) {
		LocalActionContextImpl<UserResponse> ac = createContext(UserResponse.class, parameters);
		authRestHandler.handleMe(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<NodeListResponse> ac = createContext(NodeListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<UserListResponse> ac = createContext(UserListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchUsersRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<GroupListResponse> ac = createContext(GroupListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchGroupsRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<RoleListResponse> ac = createContext(RoleListResponse.class, parameters);
		// TODO add handler
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchRolesRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<ProjectListResponse> ac = createContext(ProjectListResponse.class, parameters);
		// TODO add handler
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchProjectsRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<TagFamilyListResponse> ac = createContext(TagFamilyListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String projectName, String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<SchemaListResponse> ac = createContext(SchemaListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchSchemasRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaListResponse> ac = createContext(MicroschemaListResponse.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectNode> searchMicroschemasRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexClear(ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		adminIndexHandler.handleClear(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeIndexSync(ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		adminIndexHandler.handleSync(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SearchStatusResponse> searchStatus() {
		LocalActionContextImpl<SearchStatusResponse> ac = createContext(SearchStatusResponse.class);
		adminIndexHandler.handleStatus(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MeshStatusResponse> meshStatus() {
		LocalActionContextImpl<MeshStatusResponse> ac = createContext(MeshStatusResponse.class);
		adminHandler.handleMeshStatus(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ClusterStatusResponse> clusterStatus() {
		LocalActionContextImpl<ClusterStatusResponse> ac = createContext(ClusterStatusResponse.class);
		adminHandler.handleClusterStatus(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ClusterConfigResponse> loadClusterConfig() {
		LocalActionContextImpl<ClusterConfigResponse> ac = createContext(ClusterConfigResponse.class);
		adminHandler.handleLoadClusterConfig(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ClusterConfigResponse> updateClusterConfig(ClusterConfigRequest request) {
		LocalActionContextImpl<ClusterConfigResponse> ac = createContext(ClusterConfigResponse.class);
		adminHandler.handleUpdateClusterConfig(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request) {
		LocalActionContextImpl<MicroschemaResponse> ac = createContext(MicroschemaResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleCreate(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaResponse> createMicroschema(String uuid, MicroschemaCreateRequest request) {
		LocalActionContextImpl<MicroschemaResponse> ac = createContext(MicroschemaResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters) {
		LocalActionContextImpl<MicroschemaResponse> ac = createContext(MicroschemaResponse.class, parameters);
		microschemaCrudHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class, parameters);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleUpdate(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> deleteMicroschema(String uuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, MicroschemaModel request) {
		LocalActionContextImpl<SchemaChangesListModel> ac = createContext(SchemaChangesListModel.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleDiff(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version, String fieldKey,
		byte[] fileData, String fileName, String contentType, ParameterProvider... parameters) {

		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class);
		ac.setProject(projectName);

		MultiMap attributes = MultiMap.caseInsensitiveMultiMap();
		attributes.add("language", languageTag);
		attributes.add("version", version);

		Runnable task = () -> {

			File tmpFile = new File(System.getProperty("java.io.tmpdir"), UUIDUtil.randomUUID() + ".upload");
			vertx.fileSystem().writeFileBlocking(tmpFile.getAbsolutePath(), Buffer.buffer(fileData));
			ac.getFileUploads().add(new FileUpload() {

				@Override
				public String uploadedFileName() {
					return tmpFile.getAbsolutePath();
				}

				@Override
				public long size() {
					return fileData.length;
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

				@Override
				public boolean cancel() {
					return false;
				}
			});

			fieldAPIHandler.handleUpdateField(ac, nodeUuid, fieldKey, attributes);
		};
		new Thread(task).start();
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String nodeVersion,
		String fieldKey, InputStream fileData, long fileSize, String fileName, String contentType, boolean publish, ParameterProvider... parameters) {
		try {
			return updateNodeBinaryField(projectName, nodeUuid, languageTag, nodeVersion, fieldKey, IOUtils.toByteArray(fileData), fileName,
				contentType, parameters);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
		ParameterProvider... parameters) {
		// LocalActionContextImpl<NodeResponse> ac = createContext();
		// return new MeshLocalRequestImpl<>( ac.getFuture());
		return null;
	}

	@Override
	public MeshRequest<MeshBinaryResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey, long from,
		long to, ParameterProvider... parameters) {
		return null;
	}

	@Override
	public MeshRequest<NodeResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version,
		String fieldKey, ImageManipulationParameters imageManipulationParameter) {
		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> updateNodeBinaryFieldCheckStatus(String projectName, String nodeUuid, String languageTag, String nodeVersion,
			String fieldKey, String secret, String branchUuid, BinaryCheckStatus status, String reason) {
		return null;
	}

	@Override
	public MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryUploadRequest request, ParameterProvider... parameters) {

		LocalActionContextImpl<S3RestResponse> ac = createContext(S3RestResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(request);

		Runnable task = () -> {
			s3fieldAPIHandler.handleUpdateField(ac, nodeUuid, fieldKey);
		};
		new Thread(task).start();
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryMetadataRequest request, ParameterProvider... parameters) {

		LocalActionContextImpl<NodeResponse> ac = createContext(NodeResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(request);

		Runnable task = () -> {
			s3BinaryMetadataExtractionHandler.handleMetadataExtraction(ac, nodeUuid, fieldKey);
		};
		new Thread(task).start();
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
	public MeshWebsocket eventbus() {
		return null;
	}

	@Override
	public MeshRestClient setLogin(String username, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRestClient setLogin(String username, String password, String newPassword) {
		return null;
	}

	@Override
	public MeshRestClient setAPIKey(String apiKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAPIKey() {
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
		LocalActionContextImpl<T> ac = new LocalActionContextImpl<>(boot, user, responseType, parameters);
		return ac;
	}

	/**
	 * Add the project to the list of projects which will be used by the context object.
	 *
	 * @param name
	 * @param project
	 */
	public void addProject(String name, HibProject project) {
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
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
		ac.setProject(projectName);
		nodeCrudHandler.handleTakeOffline(ac, nodeUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
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
	public MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> createBranch(String projectName, String uuid, BranchCreateRequest branchCreateRequest,
		ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters) {
		LocalActionContextImpl<BranchListResponse> ac = createContext(BranchListResponse.class, parameters);
		ac.setProject(projectName);
		branchCrudHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<BranchResponse> updateBranch(String projectName, String branchUuid, BranchUpdateRequest request) {
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
		LocalActionContextImpl<BranchInfoSchemaList> ac = createContext(BranchInfoSchemaList.class);
		ac.setProject(projectName);
		ac.setPayloadObject(schemaVersionReferences);
		branchCrudHandler.handleAssignSchemaVersion(ac, branchUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
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
		LocalActionContextImpl<BranchResponse> ac = createContext(BranchResponse.class);
		ac.setProject(projectName);
		branchCrudHandler.handleAddTag(ac, branchUuid, tagUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> removeTagFromBranch(String projectName, String branchUuid, String tagUuid) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class);
		ac.setProject(projectName);
		branchCrudHandler.handleRemoveTag(ac, branchUuid, tagUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> findTagsForBranch(String projectName, String branchUuid, ParameterProvider... parameters) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class, parameters);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> updateTagsForBranch(String projectName, String branchUuid, TagListUpdateRequest request) {
		LocalActionContextImpl<TagListResponse> ac = createContext(TagListResponse.class);
		ac.setProject(projectName);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchNodesRaw(String projectName, String json, ParameterProvider... parameters) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class, parameters);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagsRaw(String projectName, String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<ObjectNode> searchTagFamiliesRaw(String json) {
		LocalActionContextImpl<ObjectNode> ac = createContext(ObjectNode.class);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<UserResetTokenResponse> getUserResetToken(String userUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<String> getRAML() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request, ParameterProvider... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeBackup(BackupParameters backupParameters) {
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
	public MeshRestClient enableAnonymousAccess() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public MeshRestClient disableAnonymousAccess() {
		return this;
	}

	@Override
	public MeshRestClient setAuthenticationProvider(JWTAuthentication authentication) {
		return this;
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> checkConsistency() {
		return null;
	}

	@Override
	public MeshRequest<ConsistencyCheckResponse> repairConsistency() {
		return null;
	}

	@Override
	public MeshRequest<JobListResponse> findJobs(ParameterProvider... parameters) {
		return null;
	}

	@Override
	public MeshRequest<JobResponse> findJobByUuid(String uuid) {
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteJob(String uuid) {
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> resetJob(String uuid) {
		return null;
	}

	@Override
	public MeshRequest<JobResponse> processJob(String uuid) {
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> invokeJobProcessing() {
		return null;
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateSchema(SchemaModel schema) {
		return null;
	}

	@Override
	public MeshRequest<SchemaValidationResponse> validateMicroschema(MicroschemaModel microschemaModel) {
		return null;
	}

	@Override
	public MeshRequest<PluginResponse> deployPlugin(PluginDeploymentRequest request) {
		LocalActionContextImpl<PluginResponse> ac = createContext(PluginResponse.class);
		pluginHandler.handleDeploy(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<GenericMessageResponse> undeployPlugin(String uuid) {
		LocalActionContextImpl<GenericMessageResponse> ac = createContext(GenericMessageResponse.class);
		pluginHandler.handleUndeploy(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<PluginListResponse> findPlugins(ParameterProvider... parameters) {
		LocalActionContextImpl<PluginListResponse> ac = createContext(PluginListResponse.class, parameters);
		pluginHandler.handleReadList(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<PluginResponse> findPlugin(String uuid) {
		LocalActionContextImpl<PluginResponse> ac = createContext(PluginResponse.class);
		pluginHandler.handleRead(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<LocalConfigModel> loadLocalConfig() {
		return null;
	}

	@Override
	public MeshRequest<LocalConfigModel> updateLocalConfig(LocalConfigModel localConfigModel) {
		return null;
	}

	@Override
	public JWTAuthentication getAuthentication() {
		return null;
	}

	@Override
	public MeshRestClientConfig getConfig() {
		return null;
	}

	@Override
	public MeshRequest<JsonObject> delete(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<JsonObject> get(String path) {
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
	public <R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> requestClass) {
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
	public <R> MeshRequest<R> delete(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> deleteEmpty(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> get(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> post(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> MeshRequest<R> put(String path, Class<R> responseClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshRequest<MeshBinaryResponse> debugInfo(String... params) {
		return null;
	}

	@Override
	public MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster() {
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> setCoordinationMaster() {
		return null;
	}

	@Override
	public MeshRequest<CoordinatorConfig> loadCoordinationConfig() {
		return null;
	}

	@Override
	public MeshRequest<CoordinatorConfig> updateCoordinationConfig(CoordinatorConfig coordinatorConfig) {
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> ready() {
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> live() {
		return null;
	}

	@Override
	public MeshRequest<EmptyResponse> writable() {
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> clearCache() {
		return null;
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getBranchRolePermissions(String projectName, String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		branchCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		branchCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeBranchRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		branchCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getGroupRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		groupCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantGroupRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		groupCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeGroupRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		groupCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getMicroschemaRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		microschemaCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantMicroschemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeMicroschemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		microschemaCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getNodeRolePermissions(String projectName, String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		nodeCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		nodeCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeNodeRolePermissions(String projectName, String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		nodeCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getProjectRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		projectCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantProjectRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		projectCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeProjectRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		projectCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getRoleRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		roleCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantRoleRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeRoleRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		roleCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getSchemaRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		schemaCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantSchemaRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeSchemaRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		schemaCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagFamilyRolePermissions(String projectName, String tagFamilyUuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagFamilyCrudHandler.handleReadPermissions(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagFamilyRolePermissions(String projectName, String tagFamilyUuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		ac.setPayloadObject(request);
		tagFamilyCrudHandler.handleGrantPermissions(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagFamilyRolePermissions(String projectName, String tagFamilyUuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		ac.setPayloadObject(request);
		tagFamilyCrudHandler.handleRevokePermissions(ac, tagFamilyUuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		tagCrudHandler.handleReadPermissions(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		ac.setPayloadObject(request);
		tagCrudHandler.handleGrantPermissions(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeTagRolePermissions(String projectName, String tagFamilyUuid,
			String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setProject(projectName);
		ac.setParameter("tagUuid", uuid);
		ac.setParameter("tagFamilyUuid", tagFamilyUuid);
		ac.setPayloadObject(request);
		tagCrudHandler.handleRevokePermissions(ac, tagFamilyUuid, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> getUserRolePermissions(String uuid) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		userCrudHandler.handleReadPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> grantUserRolePermissions(String uuid,
			ObjectPermissionGrantRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		userCrudHandler.handleGrantPermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ObjectPermissionResponse> revokeUserRolePermissions(String uuid,
			ObjectPermissionRevokeRequest request) {
		LocalActionContextImpl<ObjectPermissionResponse> ac = createContext(ObjectPermissionResponse.class);
		ac.setPayloadObject(request);
		userCrudHandler.handleRevokePermissions(ac, uuid);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ImageVariantsResponse> upsertNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ImageManipulationRequest request, ParameterProvider... parameters) {
		LocalActionContextImpl<ImageVariantsResponse> ac = createContext(ImageVariantsResponse.class, parameters);
		ac.setProject(projectName);
		ac.setPayloadObject(request);
		binaryVariantsHandler.handleUpsertBinaryFieldVariants(ac, nodeUuid, fieldKey);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<EmptyResponse> clearNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ParameterProvider... parameters) {
		LocalActionContextImpl<EmptyResponse> ac = createContext(EmptyResponse.class, parameters);
		ac.setProject(projectName);
		binaryVariantsHandler.handleDeleteBinaryFieldVariants(ac, nodeUuid, fieldKey);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ImageVariantsResponse> getNodeBinaryFieldImageVariants(String projectName, String nodeUuid,
			String fieldKey, ParameterProvider... parameters) {
		LocalActionContextImpl<ImageVariantsResponse> ac = createContext(ImageVariantsResponse.class, parameters);
		ac.setProject(projectName);
		binaryVariantsHandler.handleListBinaryFieldVariants(ac, nodeUuid, fieldKey);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}
}

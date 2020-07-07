package com.gentics.mesh.assertj;

import java.awt.image.BufferedImage;

import org.assertj.core.api.Assertions;

import com.gentics.mesh.assertj.impl.BranchAssert;
import com.gentics.mesh.assertj.impl.BranchResponseAssert;
import com.gentics.mesh.assertj.impl.BufferedImageAssert;
import com.gentics.mesh.assertj.impl.CoordinatorMasterResponseAssert;
import com.gentics.mesh.assertj.impl.DummySearchProviderAssert;
import com.gentics.mesh.assertj.impl.FieldMapAssert;
import com.gentics.mesh.assertj.impl.FieldSchemaContainerAssert;
import com.gentics.mesh.assertj.impl.GenericMessageResponseAssert;
import com.gentics.mesh.assertj.impl.GenericRestExceptionAssert;
import com.gentics.mesh.assertj.impl.GroupResponseAssert;
import com.gentics.mesh.assertj.impl.JobListResponseAssert;
import com.gentics.mesh.assertj.impl.JsonArrayAssert;
import com.gentics.mesh.assertj.impl.JsonObjectAssert;
import com.gentics.mesh.assertj.impl.LanguageAssert;
import com.gentics.mesh.assertj.impl.MeshElementEventModelAssert;
import com.gentics.mesh.assertj.impl.MeshEventModelAssert;
import com.gentics.mesh.assertj.impl.MeshRestClientMessageExceptionAssert;
import com.gentics.mesh.assertj.impl.MicronodeAssert;
import com.gentics.mesh.assertj.impl.MicronodeResponseAssert;
import com.gentics.mesh.assertj.impl.NavigationResponseAssert;
import com.gentics.mesh.assertj.impl.NodeAssert;
import com.gentics.mesh.assertj.impl.NodeGraphFieldContainerAssert;
import com.gentics.mesh.assertj.impl.NodeMeshEventModelAssert;
import com.gentics.mesh.assertj.impl.NodeResponseAssert;
import com.gentics.mesh.assertj.impl.PermissionInfoAssert;
import com.gentics.mesh.assertj.impl.ProjectResponseAssert;
import com.gentics.mesh.assertj.impl.PublishStatusModelAssert;
import com.gentics.mesh.assertj.impl.PublishStatusResponseAssert;
import com.gentics.mesh.assertj.impl.RoleResponseAssert;
import com.gentics.mesh.assertj.impl.SchemaChangeModelAssert;
import com.gentics.mesh.assertj.impl.SchemaContainerAssert;
import com.gentics.mesh.assertj.impl.SchemaCreateRequestAssert;
import com.gentics.mesh.assertj.impl.SchemaResponseAssert;
import com.gentics.mesh.assertj.impl.TagFamilyMeshEventModelAssert;
import com.gentics.mesh.assertj.impl.TagFamilyResponseAssert;
import com.gentics.mesh.assertj.impl.TagListResponseAssert;
import com.gentics.mesh.assertj.impl.TagMeshEventModelAssert;
import com.gentics.mesh.assertj.impl.TagResponseAssert;
import com.gentics.mesh.assertj.impl.UserResponseAssert;
import com.gentics.mesh.assertj.impl.WebRootResponseAssert;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.search.TrackingSearchProvider;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MeshAssertions extends Assertions {

	public static DummySearchProviderAssert assertThat(TrackingSearchProvider actual) {
		return new DummySearchProviderAssert(actual);
	}

	public static MeshEventModelAssert assertThat(MeshEventModel actual) {
		return new MeshEventModelAssert(actual);
	}

	public static MeshElementEventModelAssert assertThat(MeshElementEventModel actual) {
		return new MeshElementEventModelAssert(actual);
	}

	public static NodeMeshEventModelAssert assertThat(NodeMeshEventModel actual) {
		return new NodeMeshEventModelAssert(actual);
	}

	public static TagMeshEventModelAssert assertThat(TagMeshEventModel actual) {
		return new TagMeshEventModelAssert(actual);
	}

	public static TagFamilyMeshEventModelAssert assertThat(TagFamilyMeshEventModel actual) {
		return new TagFamilyMeshEventModelAssert(actual);
	}

	public static NodeResponseAssert assertThat(NodeResponse actual) {
		return new NodeResponseAssert(actual);
	}

	public static GroupResponseAssert assertThat(GroupResponse actual) {
		return new GroupResponseAssert(actual);
	}

	public static UserResponseAssert assertThat(UserResponse actual) {
		return new UserResponseAssert(actual);
	}

	public static RoleResponseAssert assertThat(RoleResponse actual) {
		return new RoleResponseAssert(actual);
	}

	public static ProjectResponseAssert assertThat(ProjectResponse actual) {
		return new ProjectResponseAssert(actual);
	}

	public static TagFamilyResponseAssert assertThat(TagFamilyResponse actual) {
		return new TagFamilyResponseAssert(actual);
	}

	public static TagResponseAssert assertThat(TagResponse actual) {
		return new TagResponseAssert(actual);
	}

	public static SchemaResponseAssert assertThat(SchemaResponse actual) {
		return new SchemaResponseAssert(actual);
	}

	public static JsonArrayAssert assertThat(JsonArray actual) {
		return new JsonArrayAssert(actual);
	}

	public static JsonObjectAssert assertThat(JsonObject actual) {
		return new JsonObjectAssert(actual);
	}

	public static MicronodeResponseAssert assertThat(MicronodeResponse actual) {
		return new MicronodeResponseAssert(actual);
	}

	public static NavigationResponseAssert assertThat(NavigationResponse actual) {
		return new NavigationResponseAssert(actual);
	}

	public static NodeAssert assertThat(Node actual) {
		return new NodeAssert(actual);
	}

	public static FieldSchemaContainerAssert assertThat(FieldSchemaContainer actual) {
		return new FieldSchemaContainerAssert(actual);
	}

	public static SchemaCreateRequestAssert assertThat(SchemaCreateRequest actual) {
		return new SchemaCreateRequestAssert(actual);
	}

	public static SchemaContainerAssert assertThat(GraphFieldSchemaContainer<?, ?, ?, ?> actual) {
		return new SchemaContainerAssert(actual);
	}

	public static SchemaChangeModelAssert assertThat(SchemaChangeModel actual) {
		return new SchemaChangeModelAssert(actual);
	}

	public static MicronodeAssert assertThat(Micronode actual) {
		return new MicronodeAssert(actual);
	}

	public static BranchAssert assertThat(Branch actual) {
		return new BranchAssert(actual);
	}

	public static BranchResponseAssert assertThat(BranchResponse actual) {
		return new BranchResponseAssert(actual);
	}

	public static NodeGraphFieldContainerAssert assertThat(NodeGraphFieldContainer actual) {
		return new NodeGraphFieldContainerAssert(actual);
	}

	public static FieldMapAssert assertThat(FieldMap actual) {
		return new FieldMapAssert(actual);
	}

	public static PublishStatusResponseAssert assertThat(PublishStatusResponse actual) {
		return new PublishStatusResponseAssert(actual);
	}

	public static PublishStatusModelAssert assertThat(PublishStatusModel actual) {
		return new PublishStatusModelAssert(actual);
	}

	public static PermissionInfoAssert assertThat(PermissionInfo actual) {
		return new PermissionInfoAssert(actual);
	}

	public static TagListResponseAssert assertThat(TagListResponse actual) {
		return new TagListResponseAssert(actual);
	}

	public static BufferedImageAssert assertThat(BufferedImage actual) {
		return new BufferedImageAssert(actual);
	}

	public static GenericRestExceptionAssert assertThat(GenericRestException actual) {
		return new GenericRestExceptionAssert(actual);
	}

	public static JobListResponseAssert assertThat(JobListResponse actual) {
		return new JobListResponseAssert(actual);
	}

	public static WebRootResponseAssert assertThat(MeshWebrootResponse actual) {
		return new WebRootResponseAssert(actual);
	}

	public static GenericMessageResponseAssert assertThat(GenericMessageResponse actual) {
		return new GenericMessageResponseAssert(actual);
	}

	public static LanguageAssert assertThat(Language actual) {
		return new LanguageAssert(actual);
	}

	public static MeshRestClientMessageExceptionAssert assertThat(MeshRestClientMessageException actual) {
		return new MeshRestClientMessageExceptionAssert(actual);
	}

	public static CoordinatorMasterResponseAssert assertThat(CoordinatorMasterResponse actual) {
		return new CoordinatorMasterResponseAssert(actual);
	}
}

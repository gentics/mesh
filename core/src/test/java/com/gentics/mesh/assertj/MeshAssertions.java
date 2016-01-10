package com.gentics.mesh.assertj;

import org.assertj.core.api.Assertions;

import com.gentics.mesh.assertj.impl.GroupResponseAssert;
import com.gentics.mesh.assertj.impl.NodeResponseAssert;
import com.gentics.mesh.assertj.impl.ProjectResponseAssert;
import com.gentics.mesh.assertj.impl.RoleResponseAssert;
import com.gentics.mesh.assertj.impl.SchemaResponseAssert;
import com.gentics.mesh.assertj.impl.SearchQueueAssert;
import com.gentics.mesh.assertj.impl.TagFamilyResponseAssert;
import com.gentics.mesh.assertj.impl.TagResponseAssert;
import com.gentics.mesh.assertj.impl.UserResponseAssert;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.search.impl.DummySearchProvider;

public class MeshAssertions extends Assertions {

	public static DummySearchProviderAssert assertThat(DummySearchProvider actual) {
		return new DummySearchProviderAssert(actual);
	}

	public static NodeResponseAssert assertThat(NodeResponse actual) {
		return new NodeResponseAssert(actual);
	}

	public static SearchQueueAssert assertThat(SearchQueue actual) {
		return new SearchQueueAssert(actual);
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

}

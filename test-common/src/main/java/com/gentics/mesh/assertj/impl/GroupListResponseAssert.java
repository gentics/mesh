package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;

public class GroupListResponseAssert extends AbstractAssert<GroupListResponseAssert, GroupListResponse> {
	public GroupListResponseAssert(GroupListResponse actual) {
		super(actual, GroupListResponseAssert.class);
	}

	public GroupListResponseAssert contains(String groupName) {
		assertThat(actual.getData().stream().map(GroupResponse::getName)).contains(groupName);
		return this;
	}

	public GroupListResponseAssert containsGroupWithRoles(String groupName, String... roleNames) {
		Optional<GroupResponse> group = actual.getData().stream()
			.filter(grp -> grp.getName().equals(groupName))
			.findFirst();

		assertThat(group)
			.withFailMessage("Could not find group %s", groupName)
			.isPresent();

		assertThat(group.get().getRoles().stream()
			.map(AbstractNameUuidReference::getName))
			.containsExactlyInAnyOrder(roleNames);

		return this;
	}
}

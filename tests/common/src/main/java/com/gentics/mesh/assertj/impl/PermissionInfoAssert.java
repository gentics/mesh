package com.gentics.mesh.assertj.impl;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import org.assertj.core.api.AbstractAssert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionInfoAssert extends AbstractAssert<PermissionInfoAssert, PermissionInfo> {

	public PermissionInfoAssert(PermissionInfo actual) {
		super(actual, PermissionInfoAssert.class);
	}

	public PermissionInfoAssert hasPerm(Permission... permissions) {
		List<String> hasPerm = actual.asMap().entrySet().stream().filter(p -> p.getValue() == true).map(e -> e.getKey().getName())
				.collect(Collectors.toList());
		List<String> mustHave = Arrays.asList(permissions).stream().map(e -> e.getName()).collect(Collectors.toList());
		assertThat(hasPerm).containsAll(mustHave);
		return this;
	}

	public PermissionInfoAssert hasNoPerm(Permission... permissions) {
		List<String> hasPerm = actual.asMap().entrySet().stream().filter(p -> p.getValue() == true).map(e -> e.getKey().getName())
				.collect(Collectors.toList());
		List<String> mustNotHave = Arrays.asList(permissions).stream().map(e -> e.getName()).collect(Collectors.toList());
		assertThat(hasPerm).doesNotContain(mustNotHave.toArray(new String[mustNotHave.size()]));
		return this;

	}

	public PermissionInfoAssert hasPublishPermsSet() {
		assertThat(actual.getPublish()).as("Publish perm is set").isNotNull();
		assertThat(actual.getReadPublished()).as("Read published perm is set").isNotNull();
		return this;
	}

	public PermissionInfoAssert hasNoPublishPermsSet() {
		assertThat(actual.getPublish()).as("Publish perm is not set").isNull();
		assertThat(actual.getReadPublished()).as("Read published perm is not set").isNull();
		return this;
	}
}

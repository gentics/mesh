package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;

public class PermissionInfoAssert extends AbstractAssert<PermissionInfoAssert, PermissionInfo> {

	public PermissionInfoAssert(PermissionInfo actual) {
		super(actual, PermissionInfoAssert.class);
	}

	public PermissionInfoAssert hasPerm(Permission... permissions) {
		List<String> hasPerm= actual.asMap().entrySet().stream().filter(p -> p.getValue() == true).map(e -> e.getKey().getName()).collect(Collectors.toList());
		List<String> mustHave = Arrays.asList(permissions).stream().map(e -> e.getName()).collect(Collectors.toList());
		assertThat(hasPerm).containsAll(mustHave);
		return this;
	}

}

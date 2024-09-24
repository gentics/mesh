package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for group names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class GroupNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created group
	 */
	public final static String GROUP_NAME = "testgroup";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createGroup(new GroupCreateRequest().setName(GROUP_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}

}

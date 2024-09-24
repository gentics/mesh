package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for role names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class RoleNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created role
	 */
	public final static String ROLE_NAME = "testrole";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createRole(new RoleCreateRequest().setName(ROLE_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}
}

package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for user names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class UserNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created user
	 */
	public final static String USER_NAME = "testuser";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createUser(new UserCreateRequest().setUsername(USER_NAME).setPassword(USER_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}

}

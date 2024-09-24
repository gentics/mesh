package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for microschema names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class MicroschemaNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created microschema
	 */
	public final static String MICROSCHEMA_NAME = "testmicroschema";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createMicroschema(new MicroschemaCreateRequest().setName(MICROSCHEMA_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}
}

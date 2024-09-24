package com.gentics.mesh.core;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for schema names
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class SchemaNameUniquenessTest extends AbstractNameUniquenessTest {
	/**
	 * Name of the created schema
	 */
	public final static String SCHEMA_NAME = "testschema";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createSchema(new SchemaCreateRequest().setName(SCHEMA_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		return Optional.empty();
	}
}

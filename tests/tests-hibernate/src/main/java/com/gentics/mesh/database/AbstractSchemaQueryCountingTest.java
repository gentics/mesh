package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public abstract class AbstractSchemaQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_SCHEMAS = 53;

	protected static int totalNumSchemas = NUM_SCHEMAS;

	protected static Set<String> initialSchemaUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			SchemaListResponse initialSchemas = call(() -> client().findSchemas());
			initialSchemaUuids.addAll(initialSchemas.getData().stream().map(SchemaResponse::getUuid).toList());
			totalNumSchemas += initialSchemas.getMetainfo().getTotalCount();

			// create schemas
			for (int i = 0; i < NUM_SCHEMAS; i++) {
				createSchema("schema_%d".formatted(i));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}

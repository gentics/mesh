package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;

public abstract class AbstractMicroschemaQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_MICROSCHEMAS = 53;

	protected static int totalNumMicroschemas = NUM_MICROSCHEMAS;

	protected static Set<String> initialMicroschemaUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			MicroschemaListResponse initialMicroschemas = call(() -> client().findMicroschemas());
			initialMicroschemaUuids.addAll(initialMicroschemas.getData().stream().map(MicroschemaResponse::getUuid).toList());
			totalNumMicroschemas += initialMicroschemas.getMetainfo().getTotalCount();

			// create schemas
			for (int i = 0; i < NUM_MICROSCHEMAS; i++) {
				createMicroschema("microschema_%d".formatted(i));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}

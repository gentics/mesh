package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed REST call queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class SchemaQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_SCHEMAS = 53;

	protected static int totalNumSchemas = NUM_SCHEMAS;

	protected static Set<String> initialSchemaUuids = new HashSet<>();

	protected final static Map<String, Consumer<SchemaResponse>> fieldAsserters = Map.of(
		"name", schema -> {
			assertThat(schema.getName()).as("Schema name").isNotEmpty();
		},
		"editor", schema -> {
			assertThat(schema.getEditor()).as("Schema editor").isNotNull();
		},
		"edited", schema -> {
			assertThat(schema.getEdited()).as("Schema edited").isNotEmpty();
		},
		"creator", schema -> {
			assertThat(schema.getCreator()).as("Schema creator").isNotNull();
		},
		"created", schema -> {
			assertThat(schema.getCreated()).as("Schema created").isNotEmpty();
		},
		"perms", schema -> {
			assertThat(schema.getPermissions()).as("Schema permissions").isNotNull();
		}
	);

	@Parameters(name = "{index}: field {0}, etag {1}")
	public static Collection<Object[]> parameters() throws Exception {
		Collection<Object[]> data = new ArrayList<>();
//		for (String field : fieldAsserters.keySet()) {
//			for (Boolean etag : Arrays.asList(true, false)) {
//				data.add(new Object[] {field, etag});
//			}
//		}
		data.add(new Object[] {"created", false});
		return data;
	}

	@Parameter(0)
	public String field;

	@Parameter(1)
	public boolean etag;

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

	@Test
	public void testGetAll() {
		SchemaListResponse schemaList = doTest(() -> client().findSchemas(new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 7, 1);
		assertThat(schemaList.getData()).as("List of fetched schemas").hasSize(totalNumSchemas);

		for (SchemaResponse schema : schemaList.getData()) {
			if (!initialSchemaUuids.contains(schema.getUuid())) {
				fieldAsserters.get(field).accept(schema);
			}
		}
	}

	@Test
	public void testGetPage() {
		SchemaListResponse schemaList = doTest(
				() -> client().findSchemas(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new PagingParametersImpl().setPerPage(10L)),
				7, 1);
		assertThat(schemaList.getData()).as("List of fetched schemas").hasSize(10);

		for (SchemaResponse schema : schemaList.getData()) {
			if (!initialSchemaUuids.contains(schema.getUuid())) {
				fieldAsserters.get(field).accept(schema);
			}
		}
	}
}

package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

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
public class SchemaQueryCountingTest extends AbstractSchemaQueryCountingTest {

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
		for (String field : fieldAsserters.keySet()) {
			for (Boolean etag : Arrays.asList(true, false)) {
				data.add(new Object[] {field, etag});
			}
		}
		return data;
	}

	@Parameter(0)
	public String field;

	@Parameter(1)
	public boolean etag;

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

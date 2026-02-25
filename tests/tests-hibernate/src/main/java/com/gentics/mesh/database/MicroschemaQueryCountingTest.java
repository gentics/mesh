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

import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
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
public class MicroschemaQueryCountingTest extends AbstractMicroschemaQueryCountingTest {

	protected final static Map<String, Consumer<MicroschemaResponse>> fieldAsserters = Map.of(
		"name", microschema -> {
			assertThat(microschema.getName()).as("Microschema name").isNotEmpty();
		},
		"editor", microschema -> {
			assertThat(microschema.getEditor()).as("Microschema editor").isNotNull();
		},
		"edited", microschema -> {
			assertThat(microschema.getEdited()).as("Microschema edited").isNotEmpty();
		},
		"creator", microschema -> {
			assertThat(microschema.getCreator()).as("Microschema creator").isNotNull();
		},
		"created", microschema -> {
			assertThat(microschema.getCreated()).as("Microschema created").isNotEmpty();
		},
		"perms", microschema -> {
			assertThat(microschema.getPermissions()).as("Microschema permissions").isNotNull();
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
		MicroschemaListResponse microschemaList = doTest(() -> client().findMicroschemas(new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 7, 1);
		assertThat(microschemaList.getData()).as("List of fetched microschemas").hasSize(totalNumMicroschemas);

		for (MicroschemaResponse microschema : microschemaList.getData()) {
			if (!initialMicroschemaUuids.contains(microschema.getUuid())) {
				fieldAsserters.get(field).accept(microschema);
			}
		}
	}

	@Test
	public void testGetPage() {
		MicroschemaListResponse microschemaList = doTest(
				() -> client().findMicroschemas(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new PagingParametersImpl().setPerPage(10L)),
				7, 1);
		assertThat(microschemaList.getData()).as("List of fetched microschemas").hasSize(10);

		for (MicroschemaResponse microschema : microschemaList.getData()) {
			if (!initialMicroschemaUuids.contains(microschema.getUuid())) {
				fieldAsserters.get(field).accept(microschema);
			}
		}
	}
}

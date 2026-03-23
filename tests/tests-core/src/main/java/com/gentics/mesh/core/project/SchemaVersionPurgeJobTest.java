package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.StreamUtil;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class SchemaVersionPurgeJobTest extends AbstractMeshTest {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() {
		return List.of(new Object[] { 0 }, new Object[] { 1 }, new Object[] { 2 }, new Object[] { 5 }, new Object[] { 8 });
	}

	@Parameter(0)
	public int numChanges;

	@Before
	public void setupSchemaVersions() {
		for (int i = 0; i < numChanges; i++) {
			int ii = i;
			waitForJob(() -> runAsAdmin(() -> {
				String schemaUuid = tx((Tx tx) -> schemaContainer("content").getUuid());
				SchemaUpdateRequest schemaUpdate = call(() -> client().findSchemaByUuid(schemaUuid)).toUpdateRequest();

				schemaUpdate.setDescription("SomeOtherDescription" + ii);
				schemaUpdate.setAutoPurge(true);
				return call(() -> client().updateSchema(schemaUuid, schemaUpdate));
			}));
		}
	}

	@Test
	public void testPurge() {
		String[] versionsBefore = numChanges < 1 
				? new String[] { "1.0" } 
				: IntStream.rangeClosed(0, numChanges).mapToObj(d -> "%d.0".formatted(d+1)).collect(Collectors.toList()).toArray(new String[numChanges+1]);
		String[] versionsAfter = numChanges < 1
				? versionsBefore
				: new String[] { "1.0", "%d.0".formatted(numChanges+1) };

		List<String> versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content before the test").containsExactly(versionsBefore);

		waitForJob(() -> runAsAdmin(() -> call(() -> client().purgeProject(projectUuid()))));

		versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content after the content purge").containsExactly(versionsBefore);

		waitForJob(() -> runAsAdmin(() -> call(() -> client().purgeSchemaVersions())));

		versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content after the schema versions purge").containsExactly(versionsAfter);
	}

	protected List<String> orderedVersions(Tx tx) {
		return StreamUtil.toStream(tx.schemaDao().findAllVersions(schemaContainer("content"))).map(v -> v.getVersion()).sorted((v1, v2) -> v1.compareTo(v2)).collect(Collectors.toList());
	}
}

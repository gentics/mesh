package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.StreamUtil;

public abstract class ContainerVersionPurgeJobTest<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer
		> extends AbstractMeshTest {

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
			waitForJob(() -> runAsAdmin(() -> updateContainerJob(ii)));
		}
	}

	@Test
	public void testPurge() {
		String[] versionsBefore = versionsBefore();
		String[] versionsAfter = versionsAfter();

		List<String> versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content before the test").containsExactly(versionsBefore);

		waitForJob(() -> runAsAdmin(() -> call(() -> client().purgeProject(projectUuid()))));

		versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content after the content purge").containsExactly(versionsBefore);

		waitForJob(() -> runAsAdmin(() -> call(() -> purgeJob())));

		versions = tx((Tx tx) -> orderedVersions(tx));
		assertThat(versions).as("Versions of content after the schema versions purge").containsExactly(versionsAfter);
	}

	protected List<String> orderedVersions(Tx tx) {
		return StreamUtil.toStream(findAllVersions(tx, containerSchema())).map(v -> v.getVersion()).sorted((v1, v2) -> v1.compareTo(v2)).collect(Collectors.toList());
	}

	protected abstract String[] versionsBefore();

	protected abstract String[] versionsAfter();

	protected abstract SC containerSchema();

	protected abstract GenericMessageResponse updateContainerJob(int i);

	protected abstract Iterable<? extends SCV> findAllVersions(Tx tx, SC container);

	protected abstract MeshRequest<GenericMessageResponse> purgeJob();
}

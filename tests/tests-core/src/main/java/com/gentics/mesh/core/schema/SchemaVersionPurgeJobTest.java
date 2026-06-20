package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameOrUUIDsRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class SchemaVersionPurgeJobTest extends AbstractContainerVersionPurgeJobTest<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel> {

	@Parameters(name = "{index}: {0} change(s), {1} used")
	public static Collection<Object[]> parameters() {
		return List.of(new Object[] { 0, 0 }, new Object[] { 1, 0 }, new Object[] { 2, 0 }, new Object[] { 4, 1 }, new Object[] { 6, 2 }, new Object[] { 8, 3 });
	}

	@Parameter(0)
	public int numChanges;

	@Parameter(1)
	public int numUsed;

	@Before
	public void setupSchemaVersions() {
		for (int i = 0; i < numChanges; i++) {
			int ii = i;
			waitForJob(() -> runAsAdmin(() -> updateContainerJob(ii)));
			if (versionUsed(ii)) {
				String latestVersion = tx((Tx tx) -> schemaContainer("content").getLatestVersion().getVersion());
				NodeCreateRequest rq = new NodeCreateRequest();
				rq.setLanguage("en");
				rq.setSchemaName("content");
				rq.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
				rq.getFields().put("slug", new StringFieldImpl().setString(ii + "_slug"));
				rq.getFields().put("teaser", new StringFieldImpl().setString(ii + "_slug"));
				rq.getFields().put("title", new StringFieldImpl().setString(ii + "_title"));
				rq.getFields().put("content", new HtmlFieldImpl().setHTML("<i>" + ii + "</i>"));
				call(() -> client().createNode(projectName(), rq, new VersioningParametersImpl().setVersion(latestVersion)));
			}
		}
	}

	@Override
	protected HibSchema containerSchema() { 
		return schemaContainer("content");
	}

	@Override
	protected GenericMessageResponse updateContainerJob(int i) {
		String schemaUuid = tx((Tx tx) -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaUpdate = call(() -> client().findSchemaByUuid(schemaUuid)).toUpdateRequest();

		schemaUpdate.setDescription("SomeOtherDescription" + i);
		schemaUpdate.setAutoPurge(!versionUsed(i));
		return call(() -> client().updateSchema(schemaUuid, schemaUpdate));
	}

	@Override
	protected Iterable<? extends HibSchemaVersion> findAllVersions(Tx tx, HibSchema container) {
		return tx.schemaDao().findAllVersions(container);
	}

	@Override
	protected MeshRequest<GenericMessageResponse> purgeJob() {
		return client().purgeSchemaVersions();
	}

	@Override
	protected MeshRequest<GenericMessageResponse> purgeJob(NameOrUUIDsRequest request) {
		return client().purgeSchemaVersions(request);
	}

	@Override
	protected String[] versionsBefore() {
		return numChanges < 1 
				? new String[] { "1.0" } 
				: IntStream.rangeClosed(0, numChanges).mapToObj(d -> "%d.0".formatted(d+1)).collect(Collectors.toList()).toArray(new String[numChanges+1]);
	}

	@Override
	protected String[] versionsAfter() {
		return numChanges < 1
				? versionsBefore()
				: (numUsed > 0 
						? IntStream.rangeClosed(0, numChanges).filter(ii -> ii == 0 || ii == numChanges || versionUsed(ii-1)).mapToObj(ii -> "%d.0".formatted(ii+1)).toArray(size -> new String[size])
						: new String[] { "1.0", "%d.0".formatted(numChanges+1) });
	}

	protected boolean versionUsed(int ii) {
		return numUsed > 0 && ii > 0 && (ii % (numUsed+1)) == 0;
	}
}

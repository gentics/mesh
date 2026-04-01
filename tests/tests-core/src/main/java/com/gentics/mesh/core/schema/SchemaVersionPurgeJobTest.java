package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameOrUUIDsRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class SchemaVersionPurgeJobTest extends ContainerVersionPurgeJobTest<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel> {

	@Override
	protected HibSchema containerSchema() {
		return schemaContainer("content");
	}

	@Override
	protected GenericMessageResponse updateContainerJob(int i) {
		String schemaUuid = tx((Tx tx) -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaUpdate = call(() -> client().findSchemaByUuid(schemaUuid)).toUpdateRequest();

		schemaUpdate.setDescription("SomeOtherDescription" + i);
		schemaUpdate.setAutoPurge(true);
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
				: new String[] { "1.0", "%d.0".formatted(numChanges+1) };
	}

}

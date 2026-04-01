package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameOrUUIDsRequest;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class MicroschemaVersionPurgeJobTest extends ContainerVersionPurgeJobTest<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion, MicroschemaModel> {

	@Override
	protected HibMicroschema containerSchema() {
		return microschemaContainer("vcard");
	}

	@Override
	protected GenericMessageResponse updateContainerJob(int i) {
		String schemaUuid = tx((Tx tx) -> microschemaContainer("vcard").getUuid());
		MicroschemaUpdateRequest schemaUpdate = call(() -> client().findMicroschemaByUuid(schemaUuid)).toRequest();

		schemaUpdate.setDescription("SomeOtherDescription" + i);
		return call(() -> client().updateMicroschema(schemaUuid, schemaUpdate));
	}

	@Override
	protected Iterable<? extends HibMicroschemaVersion> findAllVersions(Tx tx, HibMicroschema container) {
		return tx.microschemaDao().findAllVersions(containerSchema());
	}

	@Override
	protected MeshRequest<GenericMessageResponse> purgeJob(NameOrUUIDsRequest request) {
		return client().purgeMicroschemaVersions(request);
	}

	@Override
	protected MeshRequest<GenericMessageResponse> purgeJob() {
		return client().purgeMicroschemaVersions();
	}

	@Override
	protected String[] versionsBefore() {
		return numChanges < 1 
				? new String[] { "1.0" } 
				: IntStream.rangeClosed(0, numChanges).mapToObj(d -> "%d.0".formatted(d+1)).collect(Collectors.toList()).toArray(new String[numChanges+1]);
	}

	@Override
	protected String[] versionsAfter() {
		return new String[] { "%d.0".formatted(numChanges+1) };
	}
}

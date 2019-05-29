package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = false)
public class MicronodeMigrationEndpointTest extends AbstractMeshTest {

	/**
	 * Test a micronode migration which will be invoked by updating the microschema.
	 */
	@Test
	public void testUpdate() {
		grantAdminRole();
		String uuid = tx(() -> microschemaContainer("vcard").getUuid());
		MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(uuid));

		expect(MICROSCHEMA_UPDATED).one().match(1, MeshElementEventModelImpl.class, event -> {
			assertEquals("changed", event.getName());
			assertEquals(uuid, event.getUuid());
		});
		expect(MICROSCHEMA_BRANCH_ASSIGN).one();
		expect(MICROSCHEMA_MIGRATION_START).one();
		expect(MICROSCHEMA_MIGRATION_FINISHED).one();

		MicroschemaUpdateRequest request = microschema.toRequest();
		waitForJobs(() -> {
			request.setName("changed");
			call(() -> client().updateMicroschema(uuid, request));
		}, COMPLETED, 1);
		awaitEvents();
	}
}

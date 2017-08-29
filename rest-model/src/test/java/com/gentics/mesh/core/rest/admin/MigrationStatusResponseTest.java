package com.gentics.mesh.core.rest.admin;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.IDLE;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.STARTING;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.admin.migration.MigrationInfo;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatusResponse;

public class MigrationStatusResponseTest {

	@Test
	public void testStatusHandling() {
		MigrationStatusResponse response = new MigrationStatusResponse();
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:44Z").setStatus(COMPLETED));
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:46Z").setStatus(STARTING));
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:45Z").setStatus(COMPLETED));
		assertEquals(STARTING, response.getStatus());

		response = new MigrationStatusResponse();
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:44Z").setStatus(COMPLETED));
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:46Z").setStatus(FAILED));
		response.getMigrations().add(new MigrationInfo().setStartDate("2017-08-28T13:13:45Z").setStatus(COMPLETED));
		assertEquals(IDLE, response.getStatus());
	}
}

package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.admin.migration.MigrationInfo;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatusResponse;

public class MigrationStatusResponseAssert extends AbstractAssert<MigrationStatusResponseAssert, MigrationStatusResponse> {

	public MigrationStatusResponseAssert(MigrationStatusResponse actual) {
		super(actual, MigrationStatusResponseAssert.class);
	}

	/**
	 * Asserts that all infos lists the status as completed.
	 * 
	 * @return Fluent API
	 */
	public MigrationStatusResponseAssert listsAll(MigrationStatus status) {
		for (MigrationInfo info : actual.getMigrations()) {
			assertEquals("Migration {" + info + "}", status, info.getStatus());
		}
		return this;
	}

	/**
	 * Asserts the amount of infos within the response.
	 * 
	 * @param count
	 * @return Fluent API
	 */
	public MigrationStatusResponseAssert hasInfos(int count) {
		assertEquals("The status did not contain the expected amount of infos.", count, actual.getMigrations().size());
		return this;
	}

	/**
	 * Asserts the current global migration status.
	 * 
	 * @param idle
	 * @return Fluent API
	 */
	public MigrationStatusResponseAssert hasStatus(MigrationStatus idle) {
		assertEquals(MigrationStatus.IDLE, actual.getStatus());
		return this;
	}

}

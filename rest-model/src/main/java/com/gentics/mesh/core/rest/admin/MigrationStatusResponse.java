package com.gentics.mesh.core.rest.admin;

import com.gentics.mesh.core.rest.common.RestModel;

public class MigrationStatusResponse implements RestModel {

	MigrationStatus status;

	/**
	 * Return the status of the latest migration.
	 * 
	 * @return
	 */
	public MigrationStatus getStatus() {
		return status;
	}

	/**
	 * Set the status of the latest migration.
	 * 
	 * @param status
	 * @return
	 */
	public MigrationStatusResponse setStatus(MigrationStatus status) {
		this.status = status;
		return this;
	}

}

package com.gentics.mesh.core.rest.admin;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

public class MigrationStatusResponse implements RestModel {

	private MigrationStatus status;

	private List<MigrationInfo> migrations = new ArrayList<MigrationInfo>();

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

	/**
	 * Return the list of recently migrations.
	 * 
	 * @return
	 */
	public List<MigrationInfo> getMigrations() {
		return migrations;
	}

}

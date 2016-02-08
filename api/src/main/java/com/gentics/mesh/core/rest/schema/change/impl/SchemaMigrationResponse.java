package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest Model POJO for a schema migration report. The report contains a list of {@link ChangeMigrationReport} items.
 */
public class SchemaMigrationResponse implements RestModel {

	private List<ChangeMigrationReport> reports = new ArrayList<>();

	/**
	 * Return a list of stored reports.
	 * 
	 * @return
	 */
	public List<ChangeMigrationReport> getReports() {
		return reports;
	}
}

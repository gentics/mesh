package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Rest Model POJO for a schema changeset report. The report contains a list of {@link SchemaChangeReport} items.
 */
public class SchemaChangesetReport {

	private List<SchemaChangeReport> reports = new ArrayList<>();

	/**
	 * Return a list of stored reports.
	 * 
	 * @return
	 */
	public List<SchemaChangeReport> getReports() {
		return reports;
	}
}

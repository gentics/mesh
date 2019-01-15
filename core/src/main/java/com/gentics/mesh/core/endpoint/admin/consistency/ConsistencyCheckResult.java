package com.gentics.mesh.core.endpoint.admin.consistency;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;

public class ConsistencyCheckResult {

	private static final int MAX_RESULTS = 200;

	private long repairCount = 0;

	private List<InconsistencyInfo> results = new ArrayList<>(MAX_RESULTS);

	public long getRepairCount() {
		return repairCount;
	}

	public List<InconsistencyInfo> getResults() {
		return results;
	}

	public void addInconsistency(String msg, String uuid, InconsistencySeverity severity) {
		addInconsistency(new InconsistencyInfo().setDescription(msg).setElementUuid(uuid).setSeverity(severity));
	}

	public void addInconsistency(InconsistencyInfo info) {
		if (info.isRepaired()) {
			repairCount++;
		}
		// Keep the list of results small
		if (results.size() < MAX_RESULTS) {
			results.add(info);
		}
	}

	public void addInconsistency(String msg, String uuid, InconsistencySeverity severity, boolean repaired, RepairAction action) {
		addInconsistency(
			new InconsistencyInfo().setDescription(msg).setElementUuid(uuid).setSeverity(severity).setRepaired(repaired).setRepairAction(action));
	}

	/**
	 * Add the given results into this result.
	 * 
	 * @param results
	 * @return Fluent API
	 */
	public ConsistencyCheckResult merge(ConsistencyCheckResult... results) {
		for (ConsistencyCheckResult result : results) {
			merge(result);
		}
		return this;
	}

	/**
	 * Merge the two results into this result.
	 * 
	 * @param result
	 * @return Fluent API
	 */
	public ConsistencyCheckResult merge(ConsistencyCheckResult result) {
		getResults().addAll(result.getResults());
		repairCount += result.getRepairCount();
		return this;
	}

}

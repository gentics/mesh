package com.gentics.mesh.core.rest.admin.consistency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO which represents the result of a consistency check.
 */
public class ConsistencyCheckResponse implements RestModel {

	@JsonIgnore
	private static final int MAX_ENTRIES = 250;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the output was truncated because more than " + MAX_ENTRIES + " have been found.")
	private boolean outputTruncated = false;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Result of the consistency check.")
	private ConsistencyRating result;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Counter for repair operations")
	private Map<String, AtomicLong> repairCount = new HashMap<>();

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of found inconsistencies.")
	private List<InconsistencyInfo> inconsistencies = new ArrayList<>(MAX_ENTRIES);

	public ConsistencyRating getResult() {
		return getInconsistencies().isEmpty() ? ConsistencyRating.CONSISTENT : ConsistencyRating.INCONSISTENT;
	}

	public List<InconsistencyInfo> getInconsistencies() {
		return inconsistencies;
	}

	/**
	 * Add a new inconsistency info the list of found inconsistencies.
	 * 
	 * @param description
	 * @param uuid
	 * @param severity
	 * @param repaired
	 * @param repairAction
	 */
	public void addInconsistency(String description, String uuid, InconsistencySeverity severity, boolean repaired, RepairAction repairAction) {
		addInconsistency(new InconsistencyInfo().setDescription(description).setElementUuid(uuid).setSeverity(severity).setRepaired(repaired)
			.setRepairAction(repairAction));
	}

	/**
	 * Add a new inconsistency info the list of found inconsistencies.
	 * 
	 * @param description
	 * @param uuid
	 * @param severity
	 */
	public void addInconsistency(String description, String uuid, InconsistencySeverity severity) {
		addInconsistency(description, uuid, severity, false, RepairAction.NONE);
	}

	/**
	 * Add new inconsistency.
	 * 
	 * @param info
	 */
	public void addInconsistency(InconsistencyInfo info) {
		// Don't add more entries if the limit has been exceeded.
		if (reachedLimit()) {
			outputTruncated = true;
			return;
		}
		getInconsistencies().add(info);
	}

	public boolean reachedLimit() {
		return getInconsistencies().size() >= MAX_ENTRIES;
	}

	public boolean isOutputTruncated() {
		return outputTruncated;
	}

	public void setOutputTruncated(boolean outputTruncated) {
		this.outputTruncated = outputTruncated;
	}

	public Map<String, AtomicLong> getRepairCount() {
		return repairCount;
	}

}

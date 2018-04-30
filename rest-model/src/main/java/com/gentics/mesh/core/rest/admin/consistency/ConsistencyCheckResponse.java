package com.gentics.mesh.core.rest.admin.consistency;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO which represents the result of a consistency check.
 */
public class ConsistencyCheckResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Result of the consistency check.")
	private ConsistencyRating result;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of found inconsistencies.")
	private List<InconsistencyInfo> inconsistencies = new ArrayList<>();

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
		getInconsistencies()
			.add(new InconsistencyInfo().setDescription(description).setElementUuid(uuid).setSeverity(severity).setRepaired(repaired)
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

}

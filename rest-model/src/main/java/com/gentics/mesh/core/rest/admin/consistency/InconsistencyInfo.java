package com.gentics.mesh.core.rest.admin.consistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO which describes a found inconsistency.
 */
public class InconsistencyInfo implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Description of the inconsistency.")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Level of severity of the inconsistency.")
	private InconsistencySeverity severity;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the element which is related to the inconsistency.")
	private String elementUuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the inconsistency. This will indicate whether the inconsistency could be resolved via the repair action.")
	private boolean repaired = false;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Repair action which will attempt to fix the inconsistency. The action will only be invoked when using invoking the repair endpoint.")
	private RepairAction repairAction = RepairAction.NONE;

	/**
	 * Return the description.
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 * @return Fluent API
	 */
	public InconsistencyInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Return the severity.
	 * 
	 * @return
	 */
	public InconsistencySeverity getSeverity() {
		return severity;
	}

	/**
	 * Set the severity.
	 * 
	 * @param severity
	 * @return Fluent API
	 */
	public InconsistencyInfo setSeverity(InconsistencySeverity severity) {
		this.severity = severity;
		return this;
	}

	/**
	 * Return the element UUID.
	 * 
	 * @return
	 */
	public String getElementUuid() {
		return elementUuid;
	}

	/**
	 * Set the element uuid.
	 * 
	 * @param elementUuid
	 * @return Fluent API
	 */
	public InconsistencyInfo setElementUuid(String elementUuid) {
		this.elementUuid = elementUuid;
		return this;
	}

	/**
	 * Return the repaired flag.
	 * 
	 * @return
	 */
	public boolean isRepaired() {
		return repaired;
	}

	/**
	 * Set the repaired flag.
	 * 
	 * @param repaired
	 * @return Fluent API
	 */
	public InconsistencyInfo setRepaired(boolean repaired) {
		this.repaired = repaired;
		return this;
	}

	/**
	 * Return the repair action for the found inconsistency.
	 * 
	 * @return
	 */
	public RepairAction getRepairAction() {
		return repairAction;
	}

	/**
	 * Set the repair action for the found inconsistency.
	 * 
	 * @param repairAction
	 */
	public InconsistencyInfo setRepairAction(RepairAction repairAction) {
		this.repairAction = repairAction;
		return this;
	}

	@Override
	public String toString() {
		return String.format("%s: %s (%s)", severity, description, elementUuid);
	}
}

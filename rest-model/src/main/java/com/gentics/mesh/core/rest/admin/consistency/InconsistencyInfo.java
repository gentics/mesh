package com.gentics.mesh.core.rest.admin.consistency;

/**
 * POJO which describes a found inconsistency.
 */
public class InconsistencyInfo {

	private String description;

	private InconsistencySeverity severity;

	private String elementUuid;

	public String getDescription() {
		return description;
	}

	public InconsistencyInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	public InconsistencySeverity getSeverity() {
		return severity;
	}

	public InconsistencyInfo setSeverity(InconsistencySeverity severity) {
		this.severity = severity;
		return this;
	}

	public String getElementUuid() {
		return elementUuid;
	}

	public InconsistencyInfo setElementUuid(String elementUuid) {
		this.elementUuid = elementUuid;
		return this;
	}

}

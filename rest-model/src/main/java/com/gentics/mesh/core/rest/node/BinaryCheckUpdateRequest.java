package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

public class BinaryCheckUpdateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The new status for the binary. One of ACCEPTED or DENIED.")
	private BinaryCheckStatus status;

	@JsonProperty
	@JsonPropertyDescription("The reason why the binary was denied if applicable.")
	private String reason;

	/**
	 * Get the status of the binary check.
	 * @return The status of the binary check.
	 */
	public BinaryCheckStatus getStatus() {
		return status;
	}

	/**
	 * Set the status fot he binary check.
	 * @param status The status fot he binary check.
	 * @return Fluent API.
	 */
	public BinaryCheckUpdateRequest setStatus(BinaryCheckStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Get the reason why the binary was denied.
	 * @return The reason why the binary was denied.
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Set the reason why the binary was denied.
	 * @param reason The reason why the binary was denied.
	 * @return Fluent API.
	 */
	public BinaryCheckUpdateRequest setReason(String reason) {
		this.reason = reason;
		return this;
	}
}

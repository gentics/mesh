package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * A request containing a list of entity names or UUIDs.
 */
public class NameOrUUIDsRequest extends ListRequest<String> {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Should the listed names/UUIDs be excluded, not included")
	private boolean excluded = false;

	public boolean isExcluded() {
		return excluded;
	}

	public NameOrUUIDsRequest setExcluded(boolean excluded) {
		this.excluded = excluded;
		return this;
	}
}

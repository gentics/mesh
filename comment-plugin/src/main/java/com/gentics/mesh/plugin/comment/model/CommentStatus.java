package com.gentics.mesh.plugin.comment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CommentStatus {
	@JsonProperty("approved")
	APPROVED,
	@JsonProperty("pending")
	PENDING,
	@JsonProperty("deleted")
	DELETED;

	public String toFieldValue() {
		return this.name().toLowerCase();
	}
}

package com.gentics.mesh.plugin.comment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AnonymousCommentField {
	@JsonProperty("email")
	EMAIL("email"),
	@JsonProperty("author")
	AUTHOR("author"),
	@JsonProperty("subject")
	SUBJECT("subject");

	private final String name;

	AnonymousCommentField(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static AnonymousCommentField fromString(String value) {
		return AnonymousCommentField.valueOf(value.toUpperCase());
	}
}

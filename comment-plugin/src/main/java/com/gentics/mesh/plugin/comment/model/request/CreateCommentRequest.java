package com.gentics.mesh.plugin.comment.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

import java.util.Optional;

public class CreateCommentRequest implements RestModel {
	@JsonProperty(required = false)
	@JsonPropertyDescription("Subject of the comment.")
	private String subject;

	@JsonProperty(required = false)
	@JsonPropertyDescription("EMail of the comment author.")
	private String email;

	@JsonProperty(required = false)
	private String author;

	@JsonProperty(required = true)
	private String comment;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Referenced node of the comment.")
	private String node;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Language of the referenced node.")
	private String language;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Uuid of the parent comment.")
	private String parentComment;

	public String getNode() {
		return node;
	}

	public CreateCommentRequest setNode(String node) {
		this.node = node;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public CreateCommentRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	public Optional<String> getParentComment() {
		return Optional.ofNullable(parentComment);
	}

	public CreateCommentRequest setParentComment(String parentComment) {
		this.parentComment = parentComment;
		return this;
	}

	public Optional<String> getSubject() {
		return Optional.ofNullable(subject);
	}

	public CreateCommentRequest setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public Optional<String> getEmail() {
		return Optional.ofNullable(email);
	}

	public CreateCommentRequest setEmail(String email) {
		this.email = email;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public CreateCommentRequest setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Optional<String> getAuthor() {
		return Optional.ofNullable(author);
	}

	public CreateCommentRequest setAuthor(String author) {
		this.author = author;
		return this;
	}
}

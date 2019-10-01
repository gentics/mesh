package com.gentics.mesh.plugin.comment.model.request;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.plugin.comment.model.CommentStatus;

import java.util.Optional;

public class UpdateCommentRequest implements RestModel {
	private String email;
	private String author;
	private String subject;
	private String comment;
	private Boolean hidden;
	private CommentStatus status;

	public Optional<String> getEmail() {
		return Optional.ofNullable(email);
	}

	public UpdateCommentRequest setEmail(String email) {
		this.email = email;
		return this;
	}

	public Optional<String> getAuthor() {
		return Optional.ofNullable(author);
	}

	public UpdateCommentRequest setAuthor(String author) {
		this.author = author;
		return this;
	}

	public Optional<String> getSubject() {
		return Optional.ofNullable(subject);
	}

	public UpdateCommentRequest setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public Optional<String> getComment() {
		return Optional.ofNullable(comment);
	}

	public UpdateCommentRequest setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Optional<CommentStatus> getStatus() {
		return Optional.ofNullable(status);
	}

	public UpdateCommentRequest setStatus(CommentStatus status) {
		this.status = status;
		return this;
	}

	public Optional<Boolean> getHidden() {
		return Optional.ofNullable(hidden);
	}

	public UpdateCommentRequest setHidden(Boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	public FieldMap toFieldMap() {
		FieldMap fieldMap = new FieldMapImpl();

		getEmail().ifPresent(email -> fieldMap.put("email", new StringFieldImpl().setString(email)));
		getAuthor().ifPresent(author -> fieldMap.put("author", new StringFieldImpl().setString(author)));
		getComment().ifPresent(comment -> fieldMap.put("comment", new StringFieldImpl().setString(comment)));
		getSubject().ifPresent(subject -> fieldMap.put("subject", new StringFieldImpl().setString(subject)));
		getHidden().ifPresent(hidden -> fieldMap.put("hidden", new BooleanFieldImpl().setValue(hidden)));
		getStatus().ifPresent(status -> fieldMap.put("status", new StringFieldImpl().setString(status.toFieldValue())));

		return fieldMap;
	}
}

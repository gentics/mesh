package com.gentics.mesh.plugin.comment.error;

import com.gentics.mesh.plugin.comment.model.AnonymousCommentField;

public class RequiredAnonymousFieldMissingError extends InputValidationError {
	public RequiredAnonymousFieldMissingError(AnonymousCommentField field) {
		super(String.format("Required field {%s} missing. Anonymous users must fill out that field.", field.getName()));
	}
}

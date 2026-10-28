package com.gentics.mesh.plugin.comment.error;

public class InvalidEmailException extends InputValidationError {
	public InvalidEmailException(String email) {
		super(String.format("The email %s is invalid. It must contain at least an @ sign", email));
	}
}

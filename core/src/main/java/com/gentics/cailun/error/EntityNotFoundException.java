package com.gentics.cailun.error;

public class EntityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 3870449235495918185L;

	public EntityNotFoundException(String message) {
		super(message);
	}

}

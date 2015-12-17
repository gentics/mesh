package com.gentics.mesh.error;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;

/**
 * @deprecated Use {@link HttpStatusCodeErrorException} with NOT_FOUND code.
 */
@Deprecated
public class EntityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 3870449235495918185L;

	public EntityNotFoundException(String message) {
		super(message);
	}

}

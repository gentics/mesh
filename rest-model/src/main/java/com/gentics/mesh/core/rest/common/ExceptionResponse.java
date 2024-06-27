package com.gentics.mesh.core.rest.common;

import java.util.stream.Collectors;

import com.gentics.mesh.util.ExceptionIterator;
import com.gentics.mesh.util.StreamUtil;

/**
 * The {@link ExceptionResponse} is a rest model class that can be used to return a useful exception stack to the user.
 */
public class ExceptionResponse extends GenericMessageResponse {

	public ExceptionResponse() {
		super();
	}

	public ExceptionResponse(String message) {
		super(message);
	}

	public ExceptionResponse(String message, Throwable failure) {
		super(message, StreamUtil.toStream(new ExceptionIterator(failure))
				.map(Throwable::getLocalizedMessage)
				.collect(Collectors.joining("\n\tcaused by:")));
	}
}

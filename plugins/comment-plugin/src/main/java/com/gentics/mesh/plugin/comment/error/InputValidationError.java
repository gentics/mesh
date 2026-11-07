package com.gentics.mesh.plugin.comment.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class InputValidationError extends CommentPluginError {
	public InputValidationError(String message) {
		super(message);
	}

	public InputValidationError(String message, Throwable cause) {
		super(message, cause);
	}

	public InputValidationError(Throwable cause) {
		super(cause);
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.BAD_REQUEST.code();
	}

	@Override
	public boolean isServerLog() {
		return false;
	}
}

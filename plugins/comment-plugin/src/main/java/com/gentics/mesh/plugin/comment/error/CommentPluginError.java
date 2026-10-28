package com.gentics.mesh.plugin.comment.error;

import io.netty.handler.codec.http.HttpResponseStatus;

// TODO Implement
public class CommentPluginError extends RuntimeException {
	public CommentPluginError(String message) {
		super(message);
	}

	public CommentPluginError(String message, Throwable cause) {
		super(message, cause);
	}

	public CommentPluginError(Throwable cause) {
		super(cause);
	}

	public int getHttpStatusCode() {
		return HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
	}

	public boolean isServerLog() {
		return true;
	}
}

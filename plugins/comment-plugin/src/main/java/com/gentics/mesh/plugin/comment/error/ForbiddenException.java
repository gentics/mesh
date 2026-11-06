package com.gentics.mesh.plugin.comment.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ForbiddenException extends CommentPluginError {
	public ForbiddenException() {
		super("Forbidden");
	}

	public ForbiddenException(Throwable cause) {
		super(cause);
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.FORBIDDEN.code();
	}
}

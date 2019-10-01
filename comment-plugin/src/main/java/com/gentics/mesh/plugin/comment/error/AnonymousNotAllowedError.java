package com.gentics.mesh.plugin.comment.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class AnonymousNotAllowedError extends CommentPluginError {
	public AnonymousNotAllowedError() {
		super("Anonymous users are not allowed to comment");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.UNAUTHORIZED.code();
	}

	@Override
	public boolean isServerLog() {
		return false;
	}
}

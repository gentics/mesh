package com.gentics.mesh.plugin.comment.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class NotFoundException extends CommentPluginError {
	public NotFoundException() {
		super("Not Found");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.NOT_FOUND.code();
	}
}

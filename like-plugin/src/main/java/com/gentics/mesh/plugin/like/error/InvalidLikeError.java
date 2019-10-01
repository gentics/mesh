package com.gentics.mesh.plugin.like.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidLikeError extends LikePluginError {

	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 4456953064903995722L;

	public InvalidLikeError(String message) {
		super(message);
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.BAD_REQUEST.code();
	}
}

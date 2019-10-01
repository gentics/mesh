package com.gentics.mesh.plugin.like.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class LikesNotAllowedError extends LikePluginError {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3164364960298323786L;

	public LikesNotAllowedError() {
		super("Anonymous likes are not allowed");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.UNAUTHORIZED.code();
	}
}

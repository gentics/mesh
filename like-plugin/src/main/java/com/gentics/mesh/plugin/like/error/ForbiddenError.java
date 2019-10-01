package com.gentics.mesh.plugin.like.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ForbiddenError extends LikePluginError {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -1946449729707228292L;

	public ForbiddenError() {
		super("Forbidden");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.FORBIDDEN.code();
	}
}

package com.gentics.mesh.plugin.favorite.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class FavoriteNotAllowedError extends FavoritePluginError {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3164364960298323786L;

	public FavoriteNotAllowedError() {
		super("Anonymous favorites are not allowed");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.UNAUTHORIZED.code();
	}
}

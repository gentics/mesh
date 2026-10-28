package com.gentics.mesh.plugin.favorite.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class FavoritePluginError extends RuntimeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8862901746842901994L;

	public FavoritePluginError(String message) {
		super(message);
	}

	public int getHttpStatusCode() {
		return HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
	}
}

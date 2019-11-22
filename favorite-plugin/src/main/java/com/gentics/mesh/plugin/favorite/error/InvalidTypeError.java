package com.gentics.mesh.plugin.favorite.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidTypeError extends FavoritePluginError {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3164364960298323786L;

	public InvalidTypeError(String type) {
		super("Type "+type+" is invalid - type may only contains letters, digits, underscores and dashes");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.BAD_REQUEST.code();
	}
}

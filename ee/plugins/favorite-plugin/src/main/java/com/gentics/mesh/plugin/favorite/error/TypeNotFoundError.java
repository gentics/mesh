package com.gentics.mesh.plugin.favorite.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class TypeNotFoundError extends FavoritePluginError {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3164364960298323786L;

	public TypeNotFoundError(String type) {
		super("Type "+type+" is not found in the configuration");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.BAD_REQUEST.code();
	}
}

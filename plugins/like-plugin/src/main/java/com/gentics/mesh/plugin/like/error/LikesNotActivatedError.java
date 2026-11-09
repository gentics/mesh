package com.gentics.mesh.plugin.like.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class LikesNotActivatedError extends LikePluginError {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1225418814287720853L;

	public LikesNotActivatedError() {
		super("Likes not activated");
	}

	@Override
	public int getHttpStatusCode() {
		return HttpResponseStatus.METHOD_NOT_ALLOWED.code();
	}
}

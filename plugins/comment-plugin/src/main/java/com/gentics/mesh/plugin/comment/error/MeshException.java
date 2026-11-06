package com.gentics.mesh.plugin.comment.error;

import com.gentics.mesh.rest.client.MeshRestClientMessageException;

public class MeshException extends CommentPluginError {
	public MeshException(MeshRestClientMessageException cause) {
		super(cause);
	}

	@Override
	public int getHttpStatusCode() {
		return ((MeshRestClientMessageException) getCause()).getStatusCode();
	}

	@Override
	public boolean isServerLog() {
		MeshRestClientMessageException ex = ((MeshRestClientMessageException) getCause());
		if (ex.getStatusCode() >= 500) {
			return true;
		} else {
			return false;
		}
	}
}

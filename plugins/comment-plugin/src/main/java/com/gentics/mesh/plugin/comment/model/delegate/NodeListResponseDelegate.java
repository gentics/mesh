package com.gentics.mesh.plugin.comment.model.delegate;

import com.gentics.mesh.core.rest.node.NodeListResponse;

public class NodeListResponseDelegate {

	private final NodeListResponse delegate;

	public NodeListResponseDelegate(NodeListResponse delegate) {
		this.delegate = delegate;
	}

	public NodeListResponse getDelegate() {
		return delegate;
	}
}

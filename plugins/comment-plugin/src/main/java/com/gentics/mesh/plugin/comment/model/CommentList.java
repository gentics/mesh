package com.gentics.mesh.plugin.comment.model;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.plugin.comment.model.delegate.NodeListResponseDelegate;

public class CommentList extends NodeListResponseDelegate {

	public CommentList(NodeListResponse delegate) {
		super(delegate);
	}
}

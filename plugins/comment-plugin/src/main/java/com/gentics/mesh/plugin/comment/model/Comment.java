package com.gentics.mesh.plugin.comment.model;

import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.comment.model.delegate.NodeResponseDelegate;

public class Comment extends NodeResponseDelegate {

	public Comment(NodeResponse delegate, PermissionInfo permissions) {
		super(delegate);
		this.setPermissions(permissions);
	}

	// TODO getters & setters

	public String getComment() {
		return getString("comment");
	}

	public CommentStatus getStatus() {
		return CommentStatus.valueOf(getString("status").toUpperCase());
	}

	public String getAuthorReference() {
		return getString("authorReference");
	}

	public Comment setEmail(String email) {
		setString("email", email);
		return this;
	}

	public Comment setAuthor(String author) {
		setString("author", author);
		return this;
	}

	public Comment setSubject(String subject) {
		setString("subject", subject);
		return this;
	}

	public Comment setComment(String comment) {
		setString("comment", comment);
		return this;
	}

	public Comment setAuthorReference(String uuid) {
		setString("authorReference", uuid);
		return this;
	}

	public boolean isHidden() {
		return getBool("hidden");
	}
}

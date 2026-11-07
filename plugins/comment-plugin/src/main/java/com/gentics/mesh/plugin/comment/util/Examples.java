package com.gentics.mesh.plugin.comment.util;

import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.example.NodeExamples;
import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.CommentStatus;

public final class Examples {

	public static final Comment createComment() {
		NodeExamples nodeExamples = new NodeExamples();
		NodeResponse delegate = nodeExamples.getNodeResponse2();
		UserReference author = nodeExamples.createUserReference();
		delegate.getFields().putString("status", CommentStatus.PENDING.toFieldValue());
		delegate.getFields().putString("author", author.getFirstName() + " " + author.getLastName());
		delegate.getFields().putString("email", author.getLastName() + "@gentics.com");
		delegate.getFields().putString("subject", "Aircraft");
		delegate.getFields().putString("comment", "Will it fly?");
		delegate.getFields().put("hidden", new BooleanFieldImpl().setValue(false));
		delegate.getFields().putString("authorReference", author.getUuid());
		
		return new Comment(delegate, PermissionInfo.noPermissions());
	}
}

package com.gentics.mesh.plugin.comment.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.comment.model.delegate.NodeResponseDelegate;

/**
 * The comment section represents the root element for a comment tree. A section is linked to the node which references the comments.
 */
public class CommentSection extends NodeResponseDelegate {

	public CommentSection(NodeResponse delegate) {
		super(delegate);
	}

	// TODO setters

	public String getNode() {
		return getFields().getStringField("node").getString();
	}

	public Optional<Boolean> isAllowAnonymous() {
		return getOptionalBool("allowAnonymous");
	}

	public Optional<Boolean> isRequiresApproval() {
		return getOptionalBool("requiresApproval");
	}

	public Optional<List<String>> getModeratorEmails() {
		return getOptionalStringList("moderatorEmails");
	}

	public Optional<List<AnonymousCommentField>> getRequiredForAnonymous() {
		return getOptionalStringList("requiredForAnonymous")
			.map(list -> list.stream().map(AnonymousCommentField::fromString).collect(Collectors.toList()));
	}

	public Optional<String> getNodeConfigField() {
		return getOptionalString("nodeConfigField");
	}
}

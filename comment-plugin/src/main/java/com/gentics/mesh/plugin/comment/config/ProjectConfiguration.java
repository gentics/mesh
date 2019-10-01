package com.gentics.mesh.plugin.comment.config;

import com.gentics.mesh.plugin.comment.model.AnonymousCommentField;

import java.util.List;
import java.util.Optional;

public class ProjectConfiguration {
	private Boolean allowAnonymous;
	private Boolean requiresApproval;
	private List<String> moderatorEmails;
	private List<AnonymousCommentField> requiredForAnonymous;
	private String commentRoot;
	private String nodeConfigField;

	public Optional<Boolean> isAllowAnonymous() {
		return Optional.ofNullable(allowAnonymous);
	}

	public ProjectConfiguration setAllowAnonymous(boolean allowAnonymous) {
		this.allowAnonymous = allowAnonymous;
		return this;
	}

	public Optional<Boolean> isRequiresApproval() {
		return Optional.ofNullable(requiresApproval);
	}

	public ProjectConfiguration setRequiresApproval(boolean requiresApproval) {
		this.requiresApproval = requiresApproval;
		return this;
	}

	public Optional<List<String>> getModeratorEmails() {
		return Optional.ofNullable(moderatorEmails);
	}

	public ProjectConfiguration setModeratorEmails(List<String> moderatorEmails) {
		this.moderatorEmails = moderatorEmails;
		return this;
	}

	public Optional<List<AnonymousCommentField>> getRequiredForAnonymous() {
		return Optional.ofNullable(requiredForAnonymous);
	}

	public ProjectConfiguration setRequiredForAnonymous(List<AnonymousCommentField> requiredForAnonymous) {
		this.requiredForAnonymous = requiredForAnonymous;
		return this;
	}

	public Optional<String> getCommentRoot() {
		return Optional.ofNullable(commentRoot);
	}

	public ProjectConfiguration setCommentRoot(String commentRoot) {
		this.commentRoot = commentRoot;
		return this;
	}

	public Optional<String> getNodeConfigField() {
		return Optional.ofNullable(nodeConfigField);
	}

	public ProjectConfiguration setNodeConfigField(String nodeConfigField) {
		this.nodeConfigField = nodeConfigField;
		return this;
	}
}

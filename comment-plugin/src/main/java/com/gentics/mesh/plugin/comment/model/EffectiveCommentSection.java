package com.gentics.mesh.plugin.comment.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.comment.model.delegate.NodeResponseDelegate;

public class EffectiveCommentSection extends NodeResponseDelegate {

	public EffectiveCommentSection(NodeResponse delegate) {
		super(delegate);
	}

	public List<AnonymousCommentField> getRequiredForAnonymous() {
		return getFields().getStringFieldList("requiredForAnonymous").getItems().stream()
			.map(AnonymousCommentField::fromString)
			.collect(Collectors.toList());
	}

	public EffectiveCommentSection setAllowAnonymous(boolean anonymous) {
		setBool("allowAnonymous", anonymous);
		return this;
	}

	public EffectiveCommentSection setRequiresApproval(boolean requiresApproval) {
		setBool("requiresApproval", requiresApproval);
		return this;
	}

	public EffectiveCommentSection setModeratorEmails(List<String> moderatorEmails) {
		setStringList("moderatorEmails", moderatorEmails);
		return this;
	}

	public EffectiveCommentSection setRequiredForAnonymous(List<AnonymousCommentField> requiredForAnonymous) {
		List<String> enumList = requiredForAnonymous.stream().map(AnonymousCommentField::getName).collect(Collectors.toList());
		setStringList("requiredForAnonymous", enumList);
		return this;
	}

	public EffectiveCommentSection setNode(String nodeUuid) {
		setString("node", nodeUuid);
		return this;
	}

	public EffectiveCommentSection setNodeConfigField(String nodeConfigField) {
		setString("nodeConfigField", nodeConfigField);
		return this;
	}

	public boolean isRequiresApproval() {
		return getBool("requiresApproval");
	}

	public boolean allowAnonymous() {
		return getBool("allowAnonymous");
	}

	// TODO optional getters

	public String getNode() {
		return getFields().getStringField("node").getString();
	}

	public List<String> getModeratorEmails() {
		return getStringList("moderatorEmails");
	}

	public Optional<String> getNodeConfigField() {
		return getOptionalString("nodeConfigField");
	}
}

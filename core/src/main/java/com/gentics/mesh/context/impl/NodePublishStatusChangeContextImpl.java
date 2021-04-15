package com.gentics.mesh.context.impl;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.context.NodePublishStatusChangeContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;

public class NodePublishStatusChangeContextImpl extends AbstractFakeUserInternalActionContext
		implements NodePublishStatusChangeContext {

	private Project project;
	
	private Node node;
	
	private Optional<String> maybeLanguageTag;
	
	private boolean publish;
	
	public void setProject(Project project) {
		this.project = project;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public void setLanguageTag(Optional<String> maybeLanguageTag) {
		this.maybeLanguageTag = maybeLanguageTag;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	public void setFireAt(ZonedDateTime fireAt) {
		this.fireAt = fireAt;
	}

	private ZonedDateTime fireAt;
	
	@Override
	public boolean isMigrationContext() {
		return false;
	}

	@Override
	public boolean isPurgeAllowed() {
		//TODO check
		return false;
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public Optional<String> getLanguage() {
		return maybeLanguageTag;
	}

	@Override
	public ZonedDateTime getFireAt() {
		return fireAt;
	}

	@Override
	public boolean isPublish() {
		return publish;
	}

	@Override
	public Project getProject() {
		return project;
	}

}

package com.gentics.mesh.core.data.search.context.impl;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;

public class MoveEntryContextImpl implements MoveEntryContext {

	private String releaseUuid;
	private ContainerType type;
	private NodeGraphFieldContainer oldContainer;
	private NodeGraphFieldContainer newContainer;

	@Override
	public String getReleaseUuid() {
		return releaseUuid;
	}

	@Override
	public MoveEntryContext setReleaseUuid(String uuid) {
		this.releaseUuid = uuid;
		return this;
	}

	@Override
	public ContainerType getContainerType() {
		return type;
	}

	@Override
	public MoveEntryContext setContainerType(ContainerType type) {
		this.type = type;
		return this;
	}

	@Override
	public NodeGraphFieldContainer getOldContainer() {
		return oldContainer;
	}

	@Override
	public MoveEntryContext setOldContainer(NodeGraphFieldContainer container) {
		this.oldContainer = container;
		return this;
	}

	@Override
	public NodeGraphFieldContainer getNewContainer() {
		return newContainer;
	}

	@Override
	public MoveEntryContext setNewContainer(NodeGraphFieldContainer container) {
		this.newContainer = container;
		return this;
	}

}

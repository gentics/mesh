package com.gentics.mesh.core.data.search.context.impl;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * @see MoveEntryContext
 */
public class MoveEntryContextImpl implements MoveEntryContext {

	private String branchUuid;
	private ContainerType type;
	private NodeFieldContainer oldContainer;
	private NodeFieldContainer newContainer;

	@Override
	public String getBranchUuid() {
		return branchUuid;
	}

	@Override
	public MoveEntryContext setBranchUuid(String uuid) {
		this.branchUuid = uuid;
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
	public NodeFieldContainer getOldContainer() {
		return oldContainer;
	}

	@Override
	public MoveEntryContext setOldContainer(NodeFieldContainer container) {
		this.oldContainer = container;
		return this;
	}

	@Override
	public NodeFieldContainer getNewContainer() {
		return newContainer;
	}

	@Override
	public MoveEntryContext setNewContainer(NodeFieldContainer container) {
		this.newContainer = container;
		return this;
	}

}

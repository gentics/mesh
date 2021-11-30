package com.gentics.mesh.core.data.search.context.impl;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * @see MoveEntryContext
 */
public class MoveEntryContextImpl implements MoveEntryContext {

	private String branchUuid;
	private ContainerType type;
	private HibNodeFieldContainer oldContainer;
	private HibNodeFieldContainer newContainer;

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
	public HibNodeFieldContainer getOldContainer() {
		return oldContainer;
	}

	@Override
	public MoveEntryContext setOldContainer(HibNodeFieldContainer container) {
		this.oldContainer = container;
		return this;
	}

	@Override
	public HibNodeFieldContainer getNewContainer() {
		return newContainer;
	}

	@Override
	public MoveEntryContext setNewContainer(HibNodeFieldContainer container) {
		this.newContainer = container;
		return this;
	}

}

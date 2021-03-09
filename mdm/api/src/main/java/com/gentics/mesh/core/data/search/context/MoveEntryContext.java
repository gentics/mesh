package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;

public interface MoveEntryContext extends EntryContext {

	String getBranchUuid();

	MoveEntryContext setBranchUuid(String uuid);

	ContainerType getContainerType();

	MoveEntryContext setContainerType(ContainerType type);

	HibNodeFieldContainer getOldContainer();

	MoveEntryContext setOldContainer(HibNodeFieldContainer container);

	HibNodeFieldContainer getNewContainer();

	MoveEntryContext setNewContainer(HibNodeFieldContainer container);

}

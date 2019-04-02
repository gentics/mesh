package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;

public interface MoveEntryContext extends EntryContext {

	String getBranchUuid();

	MoveEntryContext setBranchUuid(String uuid);

	ContainerType getContainerType();

	MoveEntryContext setContainerType(ContainerType type);

	NodeGraphFieldContainer getOldContainer();

	MoveEntryContext setOldContainer(NodeGraphFieldContainer container);

	NodeGraphFieldContainer getNewContainer();

	MoveEntryContext setNewContainer(NodeGraphFieldContainer container);

}

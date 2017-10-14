package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;

public interface MoveEntryContext extends EntryContext {

	String getReleaseUuid();

	MoveEntryContext setReleaseUuid(String uuid);

	ContainerType getContainerType();

	MoveEntryContext setContainerType(ContainerType type);

	NodeGraphFieldContainer getOldContainer();

	MoveEntryContext setOldContainer(NodeGraphFieldContainer container);

	NodeGraphFieldContainer getNewContainer();

	MoveEntryContext setNewContainer(NodeGraphFieldContainer container);

}

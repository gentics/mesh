package com.gentics.mesh.core.data.search.context;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Context information for move operation.
 */
public interface MoveEntryContext extends EntryContext {

	/**
	 * Return the branch uuid.
	 * 
	 * @return
	 */
	String getBranchUuid();

	/**
	 * Set the branch uuid for the context.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	MoveEntryContext setBranchUuid(String uuid);

	/**
	 * Return the content type.
	 * 
	 * @return
	 */
	ContainerType getContainerType();

	/**
	 * Set the content type
	 * 
	 * @param type
	 * @return Fluent API
	 */
	MoveEntryContext setContainerType(ContainerType type);

	/**
	 * Return the old content reference.
	 * 
	 * @return
	 */
	HibNodeFieldContainer getOldContainer();

	/**
	 * Set the old content reference.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	MoveEntryContext setOldContainer(HibNodeFieldContainer container);

	/**
	 * Return the new content reference.
	 * 
	 * @return
	 */
	HibNodeFieldContainer getNewContainer();

	/**
	 * Set the new content reference.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	MoveEntryContext setNewContainer(HibNodeFieldContainer container);

}

package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;

public interface MicroschemaContainerRoot extends RootVertex<MicroschemaContainer> {

	public static final String TYPE = "microschemas";

	/**
	 * Add the microschema container to the aggregation node. The microschemas will automatically be assigned to all branches of the project to which this root
	 * belongs.
	 * 
	 * @param user
	 * @param container
	 * @param batch
	 */
	void addMicroschema(User user, MicroschemaContainer container, EventQueueBatch batch);

	/**
	 * Remove the microschema container from the aggregation node.
	 * 
	 * @param container
	 * @param batch
	 */
	void removeMicroschema(MicroschemaContainer container, EventQueueBatch batch);

	/**
	 * Check whether the given microschema is assigned to this root node.
	 * 
	 * @param microschema
	 * @return
	 */
	boolean contains(MicroschemaContainer microschema);


	MicroschemaContainer create();

	MicroschemaContainerVersion createVersion();
}

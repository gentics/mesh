package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.event.EventQueueBatch;

public interface MicroschemaRoot extends RootVertex<Microschema> {

	public static final String TYPE = "microschemas";

	/**
	 * Add the microschema container to the aggregation node. The microschemas will automatically be assigned to all branches of the project to which this root
	 * belongs.
	 * 
	 * @param user
	 * @param container
	 * @param batch
	 */
	void addMicroschema(User user, Microschema container, EventQueueBatch batch);

	/**
	 * Remove the microschema container from the aggregation node.
	 * 
	 * @param container
	 * @param batch
	 */
	void removeMicroschema(Microschema container, EventQueueBatch batch);

	/**
	 * Check whether the given microschema is assigned to this root node.
	 * 
	 * @param microschema
	 * @return
	 */
	boolean contains(Microschema microschema);


	Microschema create();

	MicroschemaVersion createVersion();
}

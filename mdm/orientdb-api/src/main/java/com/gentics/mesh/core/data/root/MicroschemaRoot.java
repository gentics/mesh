package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Root element for microschemas.
 */
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
	void addMicroschema(HibUser user, HibMicroschema container, EventQueueBatch batch);

	/**
	 * Remove the microschema container from the aggregation node.
	 * 
	 * @param container
	 * @param batch
	 */
	void removeMicroschema(HibMicroschema container, EventQueueBatch batch);

	/**
	 * Check whether the given microschema is assigned to this root node.
	 * 
	 * @param microschema
	 * @return
	 */
	boolean contains(HibMicroschema microschema);

	/**
	 * Create a new microschema.
	 * 
	 * @return
	 */
	Microschema create();

	/**
	 * Create a new microschema version.
	 * 
	 * @return
	 */
	MicroschemaVersion createVersion();
}

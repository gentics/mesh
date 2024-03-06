package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Root element for microschemas.
 */
public interface MicroschemaRoot extends RootVertex<Microschema> {

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
}

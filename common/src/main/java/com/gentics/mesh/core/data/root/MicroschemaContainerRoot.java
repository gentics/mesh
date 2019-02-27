package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
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
	 */
	void removeMicroschema(MicroschemaContainer container);

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default MicroschemaContainer create(MicroschemaModel microschema, User user, EventQueueBatch batch) {
		return create(microschema, user, null, batch);
	}

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param uuid
	 *            optional uuid
	 * @param batch
	 * @return
	 */
	MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid, EventQueueBatch batch);

	/**
	 * Check whether the given microschema is assigned to this root node.
	 * 
	 * @param microschema
	 * @return
	 */
	boolean contains(MicroschemaContainer microschema);

	/**
	 * Get the microschema container version from the given reference.
	 * 
	 * @param reference
	 *            reference
	 * @return
	 */
	MicroschemaContainerVersion fromReference(MicroschemaReference reference);

	/**
	 * Get the microschema container version from the given reference. Ignore the version number from the reference, but take the version from the branch
	 * instead.
	 * 
	 * @param reference
	 *            reference
	 * @param branch
	 *            branch
	 * @return
	 */
	MicroschemaContainerVersion fromReference(MicroschemaReference reference, Branch branch);
}

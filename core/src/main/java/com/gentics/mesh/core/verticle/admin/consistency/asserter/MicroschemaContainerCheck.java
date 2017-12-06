package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.Iterator;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Microschema container specific consistency checks.
 */
public class MicroschemaContainerCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends MicroschemaContainer> it = db.getVerticesForType(MicroschemaContainerImpl.class);
		while (it.hasNext()) {
			checkMicroschemaContainer(it.next(), response);
		}
	}

	private void checkMicroschemaContainer(MicroschemaContainer microschemaContainer, ConsistencyCheckResponse response) {
		String uuid = microschemaContainer.getUuid();

		checkOut(microschemaContainer, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		checkOut(microschemaContainer, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (microschemaContainer.getCreationTimestamp() == null) {
			response.addInconsistency("The microschemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (microschemaContainer.getLastEditedTimestamp() == null) {
			response.addInconsistency("The microschemaContainer edit timestamp is not set", uuid, MEDIUM);
		}

	}

}

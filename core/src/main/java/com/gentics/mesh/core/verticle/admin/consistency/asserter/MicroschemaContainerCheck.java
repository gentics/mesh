package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.Iterator;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
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

		if (microschemaContainer.getCreationTimestamp() == null) {
			response.addInconsistency("The microschemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (microschemaContainer.getCreator() == null) {
			response.addInconsistency("The microschemaContainer creator is not set", uuid, MEDIUM);
		}
		if (microschemaContainer.getEditor() == null) {
			response.addInconsistency("The microschemaContainer editor is not set", uuid, MEDIUM);
		}
		if (microschemaContainer.getLastEditedTimestamp() == null) {
			response.addInconsistency("The microschemaContainer edit timestamp is not set", uuid, MEDIUM);
		}

	}

}

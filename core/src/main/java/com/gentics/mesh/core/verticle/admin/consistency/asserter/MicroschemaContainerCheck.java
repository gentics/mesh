package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Microschema container specific consistency checks.
 */
public class MicroschemaContainerCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (MicroschemaContainer microschemaContainer : boot.microschemaContainerRoot().findAll()) {
			checkMicroschemaContainer(microschemaContainer, response);
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

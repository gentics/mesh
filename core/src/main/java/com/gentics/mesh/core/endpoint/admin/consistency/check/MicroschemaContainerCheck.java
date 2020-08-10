package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.core.data.container.impl.MicroschemaImpl;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Microschema container specific consistency checks.
 */
public class MicroschemaContainerCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "microschemas";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, MicroschemaImpl.class, (schema, result) -> {
			checkMicroschemaContainer(schema, result);
		}, attemptRepair, tx);
	}

	private void checkMicroschemaContainer(Microschema microschema, ConsistencyCheckResult result) {
		String uuid = microschema.getUuid();

		// checkOut(microschemaContainer, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(microschemaContainer, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (microschema.getCreationTimestamp() == null) {
			result.addInconsistency("The microschemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (microschema.getLastEditedTimestamp() == null) {
			result.addInconsistency("The microschemaContainer edit timestamp is not set", uuid, MEDIUM);
		}

	}

}

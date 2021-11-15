package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;

/**
 * Microschema container specific consistency checks.
 */
public abstract class MicroschemaContainerCheck<T extends HibMicroschema> extends AbstractContainerConsistencyCheck<T> {

	@Override
	public String getName() {
		return "microschemas";
	}

	@Override
	protected void checkContainer(Database db, T microschema, ConsistencyCheckResult result, boolean attemptRepair) {
		String uuid = microschema.getUuid();

		// checkOut(microschemaContainer, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(microschemaContainer, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (microschema.getCreationTimestamp() == null) {
			result.addInconsistency("The microschemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (microschema.getLastEditedTimestamp() == null) {
			result.addInconsistency("The microschemaContainer edit timestamp is not set", uuid, MEDIUM);
		}
		if (microschema.getBucketId() == null) {
			result.addInconsistency("The microschemaContainer bucket id is not set", uuid, MEDIUM);
		}


	}

}

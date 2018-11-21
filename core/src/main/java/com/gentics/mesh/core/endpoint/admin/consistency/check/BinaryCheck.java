package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.Iterator;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Binary specific consistency checks
 */
public class BinaryCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Binary> it = db.getVerticesForType(BinaryImpl.class);
		while (it.hasNext()) {
			checkBinary(it.next(), response);
		}
	}

	private void checkBinary(Binary binary, ConsistencyCheckResponse response) {
		String uuid = binary.getUuid();

		if (binary.getSHA512Sum() == null) {
			response.addInconsistency("The binary has no sha512sum", uuid, MEDIUM);
		}

		if (binary.getSize() == 0) {
			response.addInconsistency("The binary has no valid size specified", uuid, LOW);
		}

	}

}

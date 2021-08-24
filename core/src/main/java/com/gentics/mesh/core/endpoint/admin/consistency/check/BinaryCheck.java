package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;

/**
 * Binary specific consistency checks
 */
public class BinaryCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "binaries";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, BinaryImpl.class, (binary, result) -> {
			checkBinary(binary, result, attemptRepair);
		}, attemptRepair, tx);
	}

	private void checkBinary(HibBinary binary, ConsistencyCheckResult result, boolean attemptRepair) {
		String uuid = binary.getUuid();

		if (binary.getSHA512Sum() == null) {
			result.addInconsistency("The binary has no sha512sum", uuid, MEDIUM);
		}

		if (binary.getSize() < 0) {
			result.addInconsistency("The binary has no valid size specified", uuid, LOW);
		}

		boolean isLinkedToField = toGraph(binary).in("HAS_FIELD").hasNext();
		if (!isLinkedToField) {
			InconsistencyInfo info = new InconsistencyInfo().setDescription("The binary is dangling and not used by any container")
				.setElementUuid(uuid).setSeverity(MEDIUM);
			if (attemptRepair) {
				toGraph(binary).delete();
				info.setRepaired(true).setRepairAction(RepairAction.DELETE);
			}
			result.addInconsistency(info);
		}

	}

}

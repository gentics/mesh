package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Micronode consistency check
 */
public class MicronodeCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "micronodes";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, MicronodeImpl.class, (micronode, result) -> {
			checkMicronode(micronode, result, attemptRepair);
		}, attemptRepair, tx);
	}

	private void checkMicronode(MicronodeImpl node, ConsistencyCheckResult result, boolean attemptRepair) {
		String uuid = node.getUuid();

		NodeGraphFieldContainer firstFoundContainer = node.getContainer();

		if (firstFoundContainer == null) {
			InconsistencyInfo info = new InconsistencyInfo().setDescription("The micronode is dangling").setElementUuid(uuid).setSeverity(LOW);
			if (attemptRepair) {
				node.delete();
				info.setRepaired(true)
					.setRepairAction(RepairAction.DELETE);
			}
			result.addInconsistency(info);
		}

	}

}

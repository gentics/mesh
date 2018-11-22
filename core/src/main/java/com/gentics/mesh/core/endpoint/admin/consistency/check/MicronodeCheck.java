package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;

import java.util.Iterator;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;

public class MicronodeCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Micronode> it = db.getVerticesForType(MicronodeImpl.class);
		while (it.hasNext()) {
			checkMicronode(it.next(), response);
		}
	}

	private void checkMicronode(Micronode node, ConsistencyCheckResponse response) {
		String uuid = node.getUuid();

		NodeGraphFieldContainer firstFoundContainer = node.getContainer();
		if (firstFoundContainer == null) {
			response.addInconsistency("The micronode is dangling", uuid, LOW);
		}

	}

}

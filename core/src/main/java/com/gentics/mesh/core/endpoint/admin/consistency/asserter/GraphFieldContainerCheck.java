package com.gentics.mesh.core.endpoint.admin.consistency.asserter;

import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;

import java.util.Iterator;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;

public class GraphFieldContainerCheck implements ConsistencyCheck {
	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends NodeGraphFieldContainerImpl> it = db.getVerticesForType(NodeGraphFieldContainerImpl.class);
		while (it.hasNext()) {
			checkGraphFieldContainer(it.next(), response);
		}
	}

	private void checkGraphFieldContainer(NodeGraphFieldContainerImpl container, ConsistencyCheckResponse response) {
		String uuid = container.getUuid();
		if (container.getSchemaContainerVersion() == null) {
			response.addInconsistency("The GraphFieldContainer has no assigned SchemaContainerVersion",
				uuid, HIGH);
		}
	}
}

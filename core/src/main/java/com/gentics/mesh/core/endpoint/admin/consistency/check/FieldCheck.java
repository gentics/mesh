package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;

import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Contains checks for complex fields (list field).
 */
public class FieldCheck extends AbstractConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(FieldCheck.class);

	@Override
	public String getName() {
		return "fields";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		result.merge(checkListType(db, tx, NumberGraphFieldListImpl.class, "number", attemptRepair));
		result.merge(checkListType(db, tx, DateGraphFieldListImpl.class, "date", attemptRepair));
		result.merge(checkListType(db, tx, BooleanGraphFieldListImpl.class, "boolean", attemptRepair));
		result.merge(checkListType(db, tx, HtmlGraphFieldListImpl.class, "html", attemptRepair));
		result.merge(checkListType(db, tx, StringGraphFieldListImpl.class, "string", attemptRepair));
		result.merge(checkListType(db, tx, NodeGraphFieldListImpl.class, "node", attemptRepair));
		result.merge(checkListType(db, tx, MicronodeGraphFieldListImpl.class, "micronode", attemptRepair));
		return result;
	}

	private ConsistencyCheckResult checkListType(Database db, Tx tx, Class<? extends ListGraphField<?, ?, ?>> clazz, String name,
		boolean attemptRepair) {
		log.info("Checking list of type {" + name + "}");
		return processForType(db, clazz, (list, result) -> {
			checkList(list, result, name, attemptRepair);
		}, attemptRepair, tx);
	}

	private void checkList(ListGraphField<?, ?, ?> list, ConsistencyCheckResult result, String type, boolean attemptRepair) {
		String uuid = list.getUuid();
		// Check whether the list has a parent container
		Iterable<? extends NodeGraphFieldContainerImpl> nodeContainers = list.in(HAS_LIST).has(NodeGraphFieldContainerImpl.class)
			.frameExplicit(NodeGraphFieldContainerImpl.class);

		Iterable<? extends MicronodeImpl> micronodeContainers = list.in(HAS_LIST).has(MicronodeImpl.class)
			.frameExplicit(MicronodeImpl.class);
		if (!nodeContainers.iterator().hasNext() && !micronodeContainers.iterator().hasNext()) {
			InconsistencyInfo info = new InconsistencyInfo()
				.setDescription("Found dangling list of type {" + type + "} with uuid {" + uuid + "}. No parent container found.")
				.setElementUuid(uuid).setSeverity(LOW);
			if (attemptRepair) {
				list.delete();
				info.setRepaired(true)
					.setRepairAction(RepairAction.DELETE);
			}
			result.addInconsistency(info);
		}

	}

}

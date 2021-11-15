package com.gentics.mesh.core.endpoint.admin.consistency.check;

import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Contains checks for complex fields (list field).
 */
public abstract class FieldCheck extends AbstractConsistencyCheck {

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

	protected abstract void checkList(ListGraphField<?, ?, ?> list, ConsistencyCheckResult result, String type, boolean attemptRepair);
}

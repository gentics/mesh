package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.LOW;

import java.util.Iterator;

import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Contains checks for complex fields (list field).
 */
public class FieldCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response, boolean attemptRepair) {

		Iterator<? extends NumberGraphFieldList> it1 = db.getVerticesForType(NumberGraphFieldListImpl.class);
		while (it1.hasNext()) {
			checkList(it1.next(), response, "number");
		}

		Iterator<? extends DateGraphFieldList> it2 = db.getVerticesForType(DateGraphFieldListImpl.class);
		while (it2.hasNext()) {
			checkList(it2.next(), response, "date");
		}

		Iterator<? extends BooleanGraphFieldList> it3 = db.getVerticesForType(BooleanGraphFieldListImpl.class);
		while (it3.hasNext()) {
			checkList(it3.next(), response, "boolean");
		}

		Iterator<? extends HtmlGraphFieldList> it4 = db.getVerticesForType(HtmlGraphFieldListImpl.class);
		while (it4.hasNext()) {
			checkList(it4.next(), response, "html");
		}

		Iterator<? extends StringGraphFieldList> it5 = db.getVerticesForType(StringGraphFieldListImpl.class);
		while (it5.hasNext()) {
			checkList(it5.next(), response, "string");
		}

		Iterator<? extends NodeGraphFieldList> it6 = db.getVerticesForType(NodeGraphFieldListImpl.class);
		while (it6.hasNext()) {
			checkList(it6.next(), response, "node");
		}

		Iterator<? extends MicronodeGraphFieldList> it7 = db.getVerticesForType(MicronodeGraphFieldListImpl.class);
		while (it7.hasNext()) {
			checkList(it7.next(), response, "micronode");
		}

	}

	private void checkList(ListGraphField<?, ?, ?> list, ConsistencyCheckResponse response, String type) {
		String uuid = list.getUuid();
		// Check whether the list has a parent container
		Iterable<? extends NodeGraphFieldContainerImpl> nodeContainers = list.in(HAS_LIST).has(NodeGraphFieldContainerImpl.class)
			.frameExplicit(NodeGraphFieldContainerImpl.class);

		Iterable<? extends MicronodeImpl> micronodeContainers = list.in(HAS_LIST).has(MicronodeImpl.class)
			.frameExplicit(MicronodeImpl.class);
		if (!nodeContainers.iterator().hasNext() && !micronodeContainers.iterator().hasNext()) {
			response.addInconsistency("Found dangling list of type {" + type + "} with uuid {" + uuid + "}. No parent container found.", uuid, LOW);
		}

	}

}

package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.concurrent.atomic.AtomicInteger;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public interface NodeGraphFieldList extends ListGraphField<NodeGraphField, NodeFieldList, Node> {

	final Logger log = LoggerFactory.getLogger(NodeGraphFieldList.class);

	String TYPE = "node";

	FieldTransformator NODE_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NodeGraphFieldList nodeFieldList = container.getNodeList(fieldKey);
		if (nodeFieldList == null) {
			return Observable.just(null);
		} else {
			return nodeFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater NODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NodeFieldList nodeList = fieldMap.getNodeFieldList(fieldKey);
		NodeGraphFieldList graphNodeFieldList = container.getNodeList(fieldKey);
		boolean isNodeListFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeList == null);
		GraphField.failOnDeletionOfRequiredField(graphNodeFieldList, isNodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull= nodeList == null;
		GraphField.failOnMissingRequiredField(graphNodeFieldList, restIsNull, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isNodeListFieldSetToNull && graphNodeFieldList != null) {
			graphNodeFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Handle Create
		if (graphNodeFieldList == null) {
			graphNodeFieldList = container.createNodeList(fieldKey);
		}

		// Handle Update
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		// Remove all and add the listed items
		graphNodeFieldList.removeAll();
		AtomicInteger integer = new AtomicInteger();
		for (NodeFieldListItem item : nodeList.getItems()) {
			Node node = boot.nodeRoot().findByUuid(item.getUuid()).toBlocking().first();
			if (node == null) {
				throw error(BAD_REQUEST, "node_list_item_not_found", item.getUuid());
			}
			int pos = integer.getAndIncrement();
			if (log.isDebugEnabled()) {
				log.debug("Adding item {" + item.getUuid() + "} at position {" + pos + "}");
			}
			graphNodeFieldList.addItem(graphNodeFieldList.createNode(String.valueOf(pos), node));
		}

	};

	FieldGetter NODE_LIST_GETTER = (container, fieldSchema) -> {
		return container.getNodeList(fieldSchema.getName());
	};

	NodeGraphField createNode(String key, Node node);

}

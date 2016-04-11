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
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;

import rx.Observable;

public interface NodeGraphFieldList extends ListGraphField<NodeGraphField, NodeFieldList, Node> {

	String TYPE = "node";

	FieldTransformator NODE_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NodeGraphFieldList nodeFieldList = container.getNodeList(fieldKey);
		if (nodeFieldList == null) {
			return Observable.just(new NodeFieldListImpl());
		} else {
			return nodeFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater NODE_LIST_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		NodeGraphFieldList graphNodeFieldList = container.getNodeList(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, graphNodeFieldList, restField, fieldSchema, fieldKey, schema);
		NodeFieldListImpl nodeList = (NodeFieldListImpl) restField;

		if (nodeList.getItems().isEmpty()) {
			if (graphNodeFieldList != null) {
				graphNodeFieldList.removeField(container);
			}
		} else {
			graphNodeFieldList = container.createNodeList(fieldKey);
			BootstrapInitializer boot = BootstrapInitializer.getBoot();
			// Add the listed items
			AtomicInteger integer = new AtomicInteger();
			for (NodeFieldListItem item : nodeList.getItems()) {
				Node node = boot.nodeRoot().findByUuid(item.getUuid()).toBlocking().first();
				if (node == null) {
					throw error(BAD_REQUEST, "node_list_item_not_found", item.getUuid());
				}
				graphNodeFieldList.createNode(String.valueOf(integer.incrementAndGet()), node);
			}
		}
	};

	FieldGetter  NODE_LIST_GETTER = (container, fieldSchema) -> {
		return container.getNodeList(fieldSchema.getName());
	};

	NodeGraphField createNode(String key, Node node);

}

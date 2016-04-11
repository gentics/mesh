package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
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

	NodeGraphField createNode(String key, Node node);

}

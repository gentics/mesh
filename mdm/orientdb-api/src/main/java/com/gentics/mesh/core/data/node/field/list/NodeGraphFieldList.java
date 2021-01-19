package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * List definition for node list field.
 */
public interface NodeGraphFieldList extends ListGraphField<NodeGraphField, NodeFieldList, HibNode> {

	Logger log = LoggerFactory.getLogger(NodeGraphFieldList.class);

	String TYPE = "node";

	/**
	 * Add a field item to the list for the given node.
	 * 
	 * @param key
	 * @param node
	 * @return
	 */
	NodeGraphField createNode(String key, HibNode node);

}

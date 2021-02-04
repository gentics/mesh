package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface NodeGraphFieldList extends HibNodeFieldList, ListGraphField<HibNodeField, NodeFieldList, HibNode> {

	Logger log = LoggerFactory.getLogger(NodeGraphFieldList.class);

	String TYPE = "node";

}

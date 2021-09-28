package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

public interface HibNodeFieldList extends HibMicroschemaListableField, HibListField<HibNodeField, NodeFieldList, HibNode> {

	HibNodeField createNode(String key, HibNode node);
}

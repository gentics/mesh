package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<NodeGraphField, NodeFieldList, Node> implements NodeGraphFieldList {

	public static void init(Database database) {
		database.addVertexType(NodeGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public NodeGraphField createNode(String key, Node node) {
		return addItem(key, node);
	}

	@Override
	public Class<? extends NodeGraphField> getListType() {
		return NodeGraphFieldImpl.class;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		getElement().remove();
	}

	@Override
	public NodeFieldList transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		// Check whether the list should be returned in a collapsed or expanded format
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = parameters.getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		String[] lTagsArray = languageTags.toArray(new String[languageTags.size()]);

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			NodeFieldList restModel = new NodeFieldListImpl();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				Node node = item.getNode();
				if (!ac.getUser().canReadNode(ac, node)) {
					continue;
				}
				restModel.getItems().add(node.transformToRestSync(ac, level, lTagsArray));
			}

			return restModel;
		} else {
			NodeFieldList restModel = new NodeFieldListImpl();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				Node node = item.getNode();
				if (!ac.getUser().canReadNode(ac, node)) {
					continue;
				}
				restModel.add(node.toListItem(ac, lTagsArray));
			}
			return restModel;

		}

	}

	@Override
	public List<Node> getValues() {
		return getList().stream().map(NodeGraphField::getNode).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeGraphFieldList) {
			List<? extends NodeGraphField> listA = getList();
			List<? extends NodeGraphField> listB = ((NodeGraphFieldList) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		if (obj instanceof NodeFieldList) {
			List<? extends NodeGraphField> listA = getList();
			List<NodeFieldListItem> listB = ((NodeFieldList) obj).getItems();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}
}

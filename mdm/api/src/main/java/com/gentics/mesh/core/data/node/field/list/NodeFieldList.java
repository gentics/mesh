package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldListModel;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

public interface NodeFieldList extends MicroschemaListableField, ListField<NodeField, NodeFieldListModel, Node> {

	String TYPE = "node";

	/**
	 * Create a node within this node list field, at given index.
	 * 
	 * @param key
	 * @param node
	 * @return
	 */
	NodeField createNode(int index, Node node);

	default NodeField createNode(Node node) {
		return createNode(getSize(), node);
	}

	@Override
	default NodeFieldListModel transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		// Check whether the list should be returned in a collapsed or expanded format
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = parameters.getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		String[] lTagsArray = languageTags.toArray(new String[languageTags.size()]);

		UserDao userDao = Tx.get().userDao();
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			NodeFieldListModel restModel = new NodeFieldListImpl();
			for (NodeField item : getList()) {
				Node node = item.getNode();
				if (node == null || !userDao.canReadNode(ac.getUser(), ac, node)) {
					continue;
				}
				restModel.getItems().add(nodeDao.transformToRestSync(node, ac, level, lTagsArray));
			}

			return restModel;
		} else {
			NodeFieldListModel restModel = new NodeFieldListImpl();
			for (NodeField item : getList()) {
				Node node = item.getNode();
				if (node == null || !userDao.canReadNode(ac.getUser(), ac, node)) {
					continue;
				}
				restModel.add(contentDao.toListItem(node, ac, lTagsArray));
			}
			return restModel;
		}
	}

	@Override
	default List<Node> getValues() {
		return getList()
				.stream()
				.map(NodeField::getNode)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof NodeFieldList) {
			List<? extends NodeField> listA = getList();
			List<? extends NodeField> listB = ((NodeFieldList) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		if (obj instanceof NodeFieldListModel) {
			List<? extends NodeField> listA = getList();
			List<NodeFieldListItem> listB = ((NodeFieldListModel) obj).getItems();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}
}

package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

public interface HibNodeFieldList extends HibMicroschemaListableField, HibListField<HibNodeField, NodeFieldList, HibNode> {

	String TYPE = "node";

	HibNodeField createNode(String key, HibNode node);

	@Override
	default NodeFieldList transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		// Check whether the list should be returned in a collapsed or expanded format
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = parameters.getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		String[] lTagsArray = languageTags.toArray(new String[languageTags.size()]);

		UserDao userDao = Tx.get().userDao();
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		if (expandField && level < HibNode.MAX_TRANSFORMATION_LEVEL) {
			NodeFieldList restModel = new NodeFieldListImpl();
			for (HibNodeField item : getList()) {
				HibNode node = item.getNode();
				if (!userDao.canReadNode(ac.getUser(), ac, node)) {
					continue;
				}
				restModel.getItems().add(nodeDao.transformToRestSync(node, ac, level, lTagsArray));
			}

			return restModel;
		} else {
			NodeFieldList restModel = new NodeFieldListImpl();
			for (HibNodeField item : getList()) {
				HibNode node = item.getNode();
				if (!userDao.canReadNode(ac.getUser(), ac, node)) {
					continue;
				}
				restModel.add(contentDao.toListItem(node, ac, lTagsArray));
			}
			return restModel;
		}
	}

	@Override
	default List<HibNode> getValues() {
		return getList().stream().map(HibNodeField::getNode).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof HibNodeFieldList) {
			List<? extends HibNodeField> listA = getList();
			List<? extends HibNodeField> listB = ((HibNodeFieldList) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		if (obj instanceof NodeFieldList) {
			List<? extends HibNodeField> listA = getList();
			List<NodeFieldListItem> listB = ((NodeFieldList) obj).getItems();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}
}

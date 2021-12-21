package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see NodeGraphFieldList
 */
public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<HibNodeField, NodeFieldList, HibNode> implements NodeGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibNodeField createNode(String key, HibNode node) {
		return addItem(key, toGraph(node));
	}

	@Override
	public Class<? extends HibNodeField> getListType() {
		return NodeGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext context) {
		// We only need to remove the vertex. The entry are edges which will automatically be removed.
		getElement().remove();
	}

	@Override
	public NodeFieldList transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		// Check whether the list should be returned in a collapsed or expanded format
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = parameters.getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		String[] lTagsArray = languageTags.toArray(new String[languageTags.size()]);

		UserDao userDao = mesh().boot().userDao();
		NodeDao nodeDao = mesh().boot().nodeDao();

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
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
				restModel.add(((NodeImpl) node).toListItem(ac, lTagsArray));
			}
			return restModel;

		}

	}

	@Override
	public List<HibNode> getValues() {
		return getList().stream().map(HibNodeField::getNode).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeGraphFieldList) {
			List<? extends HibNodeField> listA = getList();
			List<? extends HibNodeField> listB = ((NodeGraphFieldList) obj).getList();
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

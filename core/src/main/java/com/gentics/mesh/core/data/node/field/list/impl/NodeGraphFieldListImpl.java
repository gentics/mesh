package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see NodeGraphFieldList
 */
public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<HibNodeField, NodeFieldList, HibNode> implements NodeGraphFieldList {

	public static FieldTransformer<NodeFieldList> NODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
		parentNode) -> {
		HibNodeFieldList nodeFieldList = container.getNodeList(fieldKey);
		if (nodeFieldList == null) {
			return null;
		} else {
			return nodeFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater NODE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();

		MeshComponent mesh = toGraph(container).getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		NodeFieldList nodeList = fieldMap.getNodeFieldList(fieldKey);
		HibNodeFieldList graphNodeFieldList = container.getNodeList(fieldKey);
		boolean isNodeListFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeList == null);
		HibField.failOnDeletionOfRequiredField(graphNodeFieldList, isNodeListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = nodeList == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(graphNodeFieldList, restIsNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion
		if (isNodeListFieldSetToNull && graphNodeFieldList != null) {
			graphNodeFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list.
		// This will effectively unlink the old list and create a new one.
		// Otherwise the list which is linked to old versions would be updated.
		graphNodeFieldList = container.createNodeList(fieldKey);

		// Remove all and add the listed items
		graphNodeFieldList.removeAll();

		// Handle Update
		HibProject project = tx.getProject(ac);
		AtomicInteger integer = new AtomicInteger();
		for (NodeFieldListItem item : nodeList.getItems()) {
			if (item == null) {
				throw error(BAD_REQUEST, "field_list_error_null_not_allowed", fieldKey);
			}
			HibNode node = nodeDao.findByUuid(project, item.getUuid());
			if (node == null) {
				throw error(BAD_REQUEST, "node_list_item_not_found", item.getUuid());
			}
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			String schemaName = node.getSchemaContainer().getName();

			if (!ArrayUtils.isEmpty(listFieldSchema.getAllowedSchemas())
				&& !Arrays.asList(listFieldSchema.getAllowedSchemas()).contains(schemaName)) {
				log.error("Node update not allowed since the schema {" + schemaName
					+ "} is not allowed. Allowed schemas {" + Arrays.toString(listFieldSchema.getAllowedSchemas()) + "}");
				throw error(BAD_REQUEST, "node_error_invalid_schema_field_value", fieldKey, schemaName);
			}
			int pos = integer.getAndIncrement();
			if (log.isDebugEnabled()) {
				log.debug("Adding item {" + item.getUuid() + "} at position {" + pos + "}");
			}
			graphNodeFieldList.addItem(graphNodeFieldList.createNode(String.valueOf(pos), node));
		}

	};

	public static FieldGetter NODE_LIST_GETTER = (container, fieldSchema) -> {
		return container.getNodeList(fieldSchema.getName());
	};

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

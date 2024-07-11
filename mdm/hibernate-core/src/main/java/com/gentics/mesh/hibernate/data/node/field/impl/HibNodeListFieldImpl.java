package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldListModel;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Node reference list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibNodeListFieldImpl 
			extends AbstractHibListFieldImpl<HibNodeListFieldEdgeImpl, NodeField, NodeFieldListModel, Node, UUID> 
			implements NodeFieldList {

	public HibNodeListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibNodeListFieldEdgeImpl.class);
	}

	public HibNodeListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibNodeListFieldEdgeImpl.class);
	}

	@Override
	public NodeField createNode(int index, Node node) {
		return makeFromValueAndIndex(node, index, HibernateTx.get());
	}

	/**
	 * Get the list contents as an ordered key-content UUD pairs.
	 * 
	 * @return
	 */
	public List<UUID> getNodeUuids(HibernateTx tx) {
		return stream(tx).map(edge -> edge.getNodeUuid()).collect(Collectors.toList());
	}

	/**
	 * Make an edge for the given container, field key and values.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param values
	 * @return
	 */
	public static HibNodeListFieldImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?,?,?,?,?> container, String fieldKey, List<UUID> values) {
		HibNodeListFieldImpl list = new HibNodeListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
			.mapToObj(i -> new HibNodeListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
			.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	public Field cloneTo(FieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getHTMLList, false);
		return HibNodeListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getNodeUuids(tx));
	}

	@Override
	protected HibNodeListFieldEdgeImpl makeFromValueAndIndex(Node node, int index, HibernateTx tx) {
		if (node == null) {
			return null;
		}
		HibNodeImpl nodeImpl = (HibNodeImpl) node;
		EntityManager em = tx.entityManager();
		get(index, tx).ifPresent(existing -> {
			tx.forceDelete(existing, "dbUuid", e -> e.getId());
		});
		HibNodeListFieldEdgeImpl item = getItemConstructor().provide(tx, valueOrNull(), index, getFieldKey(), nodeImpl, getContainer());
		em.persist(item);
		put(index, item, tx);
		return item;
	}

	@Override
	protected Node getValue(NodeField field) {
		return field.getNode();
	}

	@Override
	protected HibListFieldItemConstructor<HibNodeListFieldEdgeImpl, Node, UUID> getItemConstructor() {
		return HibNodeListFieldEdgeImpl::new;
	}
}

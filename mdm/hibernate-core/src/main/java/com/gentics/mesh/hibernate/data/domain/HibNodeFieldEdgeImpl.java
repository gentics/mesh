package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNodeFieldImpl;

/**
 * Node field reference.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "nodefieldref")
@NamedQueries({
	@NamedQuery(
			name = "nodefieldref.findEdgeByUuid",
			query = "select edge from nodefieldref edge " +
					"where edge.dbUuid = :uuid "),
	@NamedQuery(
			name = "nodefieldref.findNodeByEdgeUuid",
			query = "select n from node n " +
					"join nodefieldref edge on n.dbUuid = edge.valueOrUuid " +
					"where edge.dbUuid = :uuid "),
	@NamedQuery(
			name = "nodefieldref.findEdgeByNodeUuid",
			query = "select edge from nodefieldref edge " +
					"where edge.valueOrUuid = :uuid "),
	@NamedQuery(
			name = "nodefieldref.findEdgeByNodeUuids",
			query = "select edge, nod from nodefieldref edge " +
					"join node nod on nod.dbUuid = edge.valueOrUuid " +
					"where edge.valueOrUuid in :uuids "),
	@NamedQuery(
			name = "nodefieldref.deleteEdgeByNodeUuid",
			query = "delete from nodefieldref edge " +
					"where valueOrUuid = :uuid "),
	@NamedQuery(
			name = "nodefieldref.deleteEdgeByNodeUuids",
			query = "delete from nodefieldref edge " +
					"where valueOrUuid in :uuids "),
	@NamedQuery(
			name = "nodefieldref.findEdgeByContentUuidAndType",
			query = "select edge from nodefieldref edge " +
					"where edge.containerUuid = :uuid " +
					"and edge.containerType = :type "),
	@NamedQuery(
			name = "nodefieldref.removeByContainerUuids",
			query = "delete from nodefieldref where containerUuid in :containerUuids")
})
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainer", 
				columnNames = { "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
})
public class HibNodeFieldEdgeImpl extends AbstractFieldEdgeImpl<UUID> implements HibNodeFieldEdge, Serializable {

	private static final long serialVersionUID = -3437049126377536978L;

	public HibNodeFieldEdgeImpl() {
	}

	protected HibNodeFieldEdgeImpl(HibernateTx tx, String fieldKey, UUID nodeUuid, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, nodeUuid, parentFieldContainer);
	}

	@Override
	public Field cloneTo(FieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.NODE);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getNode, false);
		return new HibNodeFieldImpl(unmanagedBase, HibNodeFieldEdgeImpl.fromContainer(tx, unmanagedBase, getFieldKey(), valueOrUuid));
	}

	@Override
	public HibNodeImpl getNode() {
		return HibernateTx.get().load(valueOrUuid, HibNodeImpl.class);
	}

	@Override
	public String getStoredFieldName() {
		return getFieldKey();
	}

	@Override
	public boolean equals(Object obj) {
		return nodeFieldEquals(obj);
	}

	public Field getField() {
		return getReferencingContainers().map(c -> c.getField(getFieldKey())).findAny().orElseThrow();
	}

	/**
	 * Make an edge for the given container, with the field key and the referenced node UUID.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param nodeUuid
	 * @return
	 */
	protected static HibNodeFieldEdgeImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, UUID nodeUuid) {
		HibNodeFieldEdgeImpl edge = new HibNodeFieldEdgeImpl(tx, fieldKey, nodeUuid, container);
		tx.entityManager().persist(edge);
		return edge;
	}

	/**
	 * Make an edge for the given container, with the field key and the referenced node.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param node
	 * @return
	 */
	public static HibNodeFieldEdgeImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, HibNodeImpl node) {
		HibNodeFieldEdgeImpl edge = new HibNodeFieldEdgeImpl(tx, fieldKey, node.getDbUuid(), container);
		tx.entityManager().persist(edge);
		return edge;
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		// Node edge does not own a node, so nothing to do with it on a deletion.
	}
}

package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.database.HibernateTx;

/**
 * Node reference list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "nodelistitem")
@NamedQueries({
	@NamedQuery(name = "nodelistitem.findByContentUuidAndType", query = "select l "
			+ " from nodelistitem l "
			+ " where l.containerUuid = :contentUuid "
			+ " and l.containerType = :contentType "),
	@NamedQuery(name = "nodelistitem.findByNodeUuid", query = "select l "
			+ " from nodelistitem l "
			+ " where l.valueOrUuid = :nodeUuid "),
	@NamedQuery(name = "nodelistitem.findByNodeUuids", query = "select l, n "
			+ " from nodelistitem l "
			+ " join node n on n.dbUuid = l.valueOrUuid "
			+ " where l.valueOrUuid in :nodeUuids "),
	@NamedQuery(name = "nodelistitem.deleteByNodeUuid", query = " delete from nodelistitem "
			+ " where valueOrUuid = :nodeUuid "),
	@NamedQuery(name = "nodelistitem.deleteByNodeUuids", query = " delete from nodelistitem "
			+ " where valueOrUuid in :nodeUuids "),
})
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibNodeListFieldEdgeImpl 
		extends AbstractHibListFieldEdgeImpl<UUID> 
		implements HibNodeFieldEdge, Serializable {

	private static final long serialVersionUID = -3237898296052218580L;

	public HibNodeListFieldEdgeImpl() {
	}

	public HibNodeListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, Node node, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		this(tx, listUuid, index, fieldKey, (UUID) node.getId(), parentFieldContainer);
	}

	public HibNodeListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, UUID nodeUuid, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, nodeUuid, parentFieldContainer);
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		// No strong reference from the node = nothing to care about
	}

	public void setNode(HibNodeImpl node) {
		this.valueOrUuid = node.getDbUuid();
		HibernateTx.get().entityManager().merge(this);
	}

	public UUID getNodeUuid() {
		return valueOrUuid;
	}

	@Override
	public Node getNode() {
		return HibernateTx.get().load(valueOrUuid, HibNodeImpl.class);
	}

	@Override
	public String getStoredFieldName() {
		return getStoredFieldKey();
	}

	@Override
	public boolean equals(Object obj) {
		return nodeFieldEquals(obj);
	}

	@Override
	public String getFieldName() {
		return HibNodeFieldEdge.super.getFieldName();
	}
}

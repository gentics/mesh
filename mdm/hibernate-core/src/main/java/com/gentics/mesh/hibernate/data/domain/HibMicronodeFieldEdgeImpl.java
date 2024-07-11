package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.database.HibernateTx;

/**
 * Micronode field reference.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "micronodefieldref")
@NamedQueries({
	@NamedQuery(
			name = "micronodefieldref.findEdgeByUuid",
			query = "select edge from micronodefieldref edge " +
					"where edge.dbUuid = :uuid "),
	@NamedQuery(
			name = "micronodefieldref.findContentKeysByUuids",
			query = "select edge.dbUuid as edge_id, edge.valueOrUuid as key_id, edge.microschemaVersion.dbUuid as version_id from micronodefieldref edge " +
					"where edge.dbUuid in :uuids "),
	@NamedQuery(
			name = "micronodefieldref.findEdgeByMicronodeUuid",
			query = "select edge from micronodefieldref edge " +
					"where edge.valueOrUuid = :uuid "),
	@NamedQuery(
			name = "micronodefieldref.findByVersion",
			query = "select distinct(edge.containerUuid) from micronodefieldref edge where edge.microschemaVersion = :version"),
	@NamedQuery(
			name = "micronodefieldref.findEdgeByContentUuidAndType",
			query = "select edge from micronodefieldref edge " +
					"where edge.containerUuid = :uuid " +
					"and edge.containerType = :type "),
	@NamedQuery(
			name = "micronodefieldref.findByContainerUuids",
			query =  "select edge from micronodefieldref edge where edge.containerUuid in :containerUuids"),
	@NamedQuery(
			name = "micronodefieldref.findByMicrocontainerUuids",
			query = "select edge from micronodefieldref edge where edge.valueOrUuid in :micronodeUuids"),
	@NamedQuery(
			name = "micronodefieldref.removeByContainerUuids",
			query =  "delete from micronodefieldref where containerUuid in :containerUuids"),

})
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerMicronode", 
				columnNames = { "microschemaVersion_dbuuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
})
public class HibMicronodeFieldEdgeImpl extends AbstractFieldEdgeImpl<UUID> implements HibMicronodeFieldEdge, Serializable {

	private static final long serialVersionUID = -2372898533722905670L;

	@ManyToOne(optional = false, fetch = FetchType.LAZY, targetEntity = HibMicroschemaVersionImpl.class)
	private MicroschemaVersion microschemaVersion;

	public HibMicronodeFieldEdgeImpl() {
	}

	protected HibMicronodeFieldEdgeImpl(
				HibernateTx tx, String fieldKey, UUID micronodeUuid, MicroschemaVersion microschemaVersion,
				HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, micronodeUuid, parentFieldContainer);
		this.microschemaVersion = microschemaVersion;
	}

	@Override
	public Field cloneTo(FieldContainer container) {
		HibMicronodeFieldEdgeImpl field = new HibMicronodeFieldEdgeImpl(
				HibernateTx.get(), getFieldKey(), 
				valueOrUuid, microschemaVersion, (HibUnmanagedFieldContainer<?,?,?,?,?>) container);
		return field;
	}

	@Override
	public HibMicronodeContainerImpl getMicronode() {
		return HibernateTx.get().contentDao().getFieldContainer(microschemaVersion, valueOrUuid);
	}

	@Override
	public boolean equals(Object obj) {
		return micronodeFieldEquals(obj);
	}

	/**
	 * Make an edge for the given container, with the field key and the referenced micronode.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param micronode
	 * @return
	 */
	public static HibMicronodeFieldEdgeImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, HibMicronodeContainerImpl micronode) {
		HibMicronodeFieldEdgeImpl edge = new HibMicronodeFieldEdgeImpl(
				tx, fieldKey, 
				micronode.getDbUuid(), (HibMicroschemaVersionImpl) micronode.getSchemaContainerVersion(), container);
		tx.entityManager().persist(edge);
		return edge;
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		tx.contentDao().tryDelete(getMicronode(), this, bac);
	}

	@Override
	public String getStoredFieldKey() {
		return super.getFieldKey();
	}

	@Override
	public MicroschemaVersion getMicroschemaVersion() {
		return microschemaVersion;
	}
}

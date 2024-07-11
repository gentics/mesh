package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.database.HibernateTx;

/**
 * Micronode list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "micronodelistitem")
@NamedQueries({
	@NamedQuery(name = "micronodelistitem.deleteByNodeUuidVersion",
			query = "delete micronodelistitem "
					+ " where valueOrUuid = :micronodeUuid "
					+ " and microschemaVersion = :micronodeVersion "),
	@NamedQuery(name = "micronodelistitem.findByMicronodeUuid",
			query = "select l from micronodelistitem l "
					+ " where l.valueOrUuid = :micronodeUuid "),
	@NamedQuery(name = "micronodelistitem.findByMicronodeUuids",
			query = "select l from micronodelistitem l "
					+ " where l.valueOrUuid in :micronodeUuids "),
	@NamedQuery(name = "micronodelistitem.findByVersion",
			query = "select distinct(l.containerUuid) from micronodelistitem l "
					+ " where l.microschemaVersion = :version "),
	@NamedQuery(name = "micronodelistitem.findUniqueFieldKeysByContentUuidTypeAndVersion", 
			query = "select distinct f.fieldKey, f.listUuid from micronodelistitem f "
					+ " where f.containerUuid = :containerUuid "
					+ " and f.containerType = :containerType "),
	@NamedQuery(name = "micronodelistitem.findByContainerUuids",
			query = "select l from micronodelistitem l "
					+ " where l.containerUuid in :containerUuids"),
	@NamedQuery(name = "micronodelistitem.removeByContainerUuids",
			query = "delete from micronodelistitem where containerUuid in :containerUuids")
})
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibMicronodeListFieldEdgeImpl 
		extends AbstractHibListFieldEdgeImpl<UUID> 
		implements HibMicronodeFieldEdge, Serializable {

	private static final long serialVersionUID = 7725041530839333052L;

	@ManyToOne(targetEntity = HibMicroschemaVersionImpl.class, optional = false)
	private HibMicroschemaVersion microschemaVersion;

	public HibMicronodeListFieldEdgeImpl() {
	}

	public HibMicronodeListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, HibMicronode micronode,
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		this(tx, listUuid, index, fieldKey, (UUID) micronode.getId(), micronode.getSchemaContainerVersion(), parentFieldContainer);
	}

	public HibMicronodeListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, UUID micronodeUuid, HibMicroschemaVersion version,
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, micronodeUuid, parentFieldContainer);
		this.microschemaVersion = version;
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		HibMicronode micronode = getMicronode();
		tx.contentDao().tryDelete(micronode, this, bac);
	}

	@Override
	public HibMicronodeContainerImpl getMicronode() {
		return HibernateTx.get().contentDao().getFieldContainer(microschemaVersion, valueOrUuid);
	}

	@Override
	public boolean equals(Object obj) {
		return micronodeFieldEquals(obj);
	}

	@Override
	public String getStoredFieldKey() {
		return super.getFieldKey();
	}

	@Override
	public String getFieldKey() {
		return HibMicronodeFieldEdge.super.getFieldKey();
	}

	public HibMicroschemaVersion getMicroschemaVersion() {
		return microschemaVersion;
	}
}

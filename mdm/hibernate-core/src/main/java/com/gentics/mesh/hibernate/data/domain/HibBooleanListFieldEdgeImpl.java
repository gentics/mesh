package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.database.HibernateTx;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Boolean list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "boollistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibBooleanListFieldEdgeImpl 
		extends AbstractHibPrimitiveListFieldEdgeImpl<HibBooleanField, Boolean> implements HibBooleanField, Serializable {

	private static final long serialVersionUID = -1506906653905104325L;

	public HibBooleanListFieldEdgeImpl() {
	}

	public HibBooleanListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, Boolean value, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public Boolean getBoolean() {
		return valueOrUuid;
	}

	@Override
	public void setBoolean(Boolean bool) {
		this.valueOrUuid = bool;
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return booleanEquals(obj);
	}
}

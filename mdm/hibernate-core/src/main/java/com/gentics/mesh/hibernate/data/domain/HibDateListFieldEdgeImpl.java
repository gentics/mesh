package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.database.HibernateTx;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Date list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "datelistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibDateListFieldEdgeImpl 
		extends AbstractHibPrimitiveListFieldEdgeImpl<HibDateField, Long> implements HibDateField, Serializable {

	private static final long serialVersionUID = -6554262711404820079L;

	public HibDateListFieldEdgeImpl() {
	}

	public HibDateListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, Long value, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public Long getDate() {
		return valueOrUuid;
	}

	@Override
	public void setDate(Long value) {
		this.valueOrUuid = value;
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return dateEquals(obj);
	}
}

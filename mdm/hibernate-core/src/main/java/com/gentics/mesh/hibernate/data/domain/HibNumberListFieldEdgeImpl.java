package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.core.data.node.field.NumberField;
import com.gentics.mesh.database.HibernateTx;

/**
 * Number list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "numberlistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibNumberListFieldEdgeImpl extends AbstractHibPrimitiveListFieldEdgeImpl<NumberField, Double>
		implements NumberField, Serializable {

	private static final long serialVersionUID = -4517639730172057696L;

	public HibNumberListFieldEdgeImpl() {
	}

	public HibNumberListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, Number value,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value.doubleValue(), parentFieldContainer);
	}

	@Override
	public Number getNumber() {
		return valueOrUuid;
	}

	@Override
	public void setNumber(Number value) {
		this.valueOrUuid = value.doubleValue();
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return numberEquals(obj);
	}
}

package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.database.HibernateTx;

/**
 * String list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "stringlistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibStringListFieldEdgeImpl extends AbstractHibPrimitiveListFieldEdgeImpl<HibStringField, String>
		implements HibStringField, Serializable {

	private static final long serialVersionUID = -8019702371631385160L;

	public HibStringListFieldEdgeImpl() {
	}

	public HibStringListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, String value,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public String getString() {
		return valueOrUuid;
	}

	@Override
	public void setString(String value) {
		this.valueOrUuid = value;
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return stringEquals(obj);
	}
}

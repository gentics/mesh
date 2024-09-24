package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.database.HibernateTx;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * HTML list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "htmllistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibHtmlListFieldEdgeImpl extends AbstractHibPrimitiveListFieldEdgeImpl<HibHtmlField, String>
		implements HibHtmlField, Serializable {

	private static final long serialVersionUID = -6611123692017623250L;

	public HibHtmlListFieldEdgeImpl() {
	}

	public HibHtmlListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, String value,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public String getHTML() {
		return valueOrUuid;
	}

	@Override
	public void setHtml(String value) {
		this.valueOrUuid = value;
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return htmlEquals(obj);
	}
}

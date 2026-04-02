package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.database.HibernateTx;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JSON object list field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "jsonlistitem")
@Table(uniqueConstraints = { 
		@UniqueConstraint(
				name = "KeyTypeVersionContainerListIndex", 
				columnNames = { "itemIndex", "listUuid", "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
		)
}, indexes = {
		@Index(columnList = "listUuid")
})
public class HibJsonListFieldEdgeImpl extends AbstractHibPrimitiveListFieldEdgeImpl<HibJsonField, JsonObject> implements HibJsonField, Serializable {

	private static final long serialVersionUID = -6554262711404820079L;

	public HibJsonListFieldEdgeImpl() {
	}

	public HibJsonListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, JsonObject value, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public JsonObject getJson() {
		return valueOrUuid;
	}

	@Override
	public void setJson(JsonObject value) {
		this.valueOrUuid = value;
		HibernateTx.get().entityManager().merge(this);
	}

	@Override
	public boolean equals(Object obj) {
		return jsonEquals(obj);
	}
}

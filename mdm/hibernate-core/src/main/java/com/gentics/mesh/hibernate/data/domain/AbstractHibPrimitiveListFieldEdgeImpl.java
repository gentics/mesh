package com.gentics.mesh.hibernate.data.domain;

import java.util.UUID;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.database.HibernateTx;

import jakarta.persistence.MappedSuperclass;

/**
 * A base for the storeable list item of primitive lists. 
 * 
 * @author plyhun
 *
 * @param <LF> supported list field type
 * @param <U> a primitive value type
 */
@MappedSuperclass
public abstract class AbstractHibPrimitiveListFieldEdgeImpl<LF extends HibListableField, U> 
		extends AbstractHibListFieldEdgeImpl<U> {

	public AbstractHibPrimitiveListFieldEdgeImpl() {
		super();
	}

	public AbstractHibPrimitiveListFieldEdgeImpl(HibernateTx tx, UUID listUuid, int index, String fieldKey, U value, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, listUuid, index, fieldKey, value, parentFieldContainer);
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx) {
		// no extra actions for primitives
	}
}

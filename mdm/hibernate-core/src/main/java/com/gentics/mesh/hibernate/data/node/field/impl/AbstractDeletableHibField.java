package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.UUID;

import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibFieldEdge;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * An extension to {@link AbstractReferenceHibField} with the deletion marker of {@link HibDeletableField}.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class AbstractDeletableHibField<T extends HibFieldEdge> extends AbstractReferenceHibField<T> implements HibDeletableField {

	public AbstractDeletableHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent,
			FieldTypes fieldTypes, T initialValue) {
		super(fieldKey, parent, fieldTypes, initialValue);
	}

	public AbstractDeletableHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent,
			FieldTypes fieldTypes, UUID initialValue, Class<T> referencedClass) {
		super(fieldKey, parent, fieldTypes, initialValue, referencedClass);
	}
}

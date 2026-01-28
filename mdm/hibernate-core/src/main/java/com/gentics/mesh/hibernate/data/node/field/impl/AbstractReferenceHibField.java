package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibFieldEdge;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * A reference field implementation.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class AbstractReferenceHibField<T extends HibFieldEdge> extends AbstractBasicHibField<UUID> {
	
	protected final Class<T> referencedClass;

	/**
	 * Initial setter constructor. We ensure that the referenced element exists.
	 * 
	 * @param fieldKey
	 * @param parent
	 * @param fieldTypes
	 * @param initialValue
	 */
	@SuppressWarnings("unchecked")
	public AbstractReferenceHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, FieldTypes fieldTypes, @Nonnull T initialValue) {
		this(fieldKey, parent, fieldTypes, initialValue.getDbUuid(), (Class<T>) initialValue.getClass());
		storeValue(value.get());
	}

	/**
	 * Getter constructor, used by the container field getter. 
	 * 
	 * @param fieldKey
	 * @param parent
	 * @param fieldTypes
	 * @param initialValue
	 */
	protected AbstractReferenceHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, FieldTypes fieldTypes, UUID initialValue, Class<T> referencedClass) {
		super(fieldKey, parent, fieldTypes, initialValue);
		this.referencedClass = referencedClass;
	}

	/**
	 * Get the referenced entity.
	 * 
	 * @return
	 */
	abstract public T getReferencedEdge();

	@Override
	public void onFieldDeleted(HibernateTx tx) {
		T referenced = getReferencedEdge();
		if (referenced != null) {
			referenced.onEdgeDeleted(tx);
			tx.delete(referenced);
		}
	}
}

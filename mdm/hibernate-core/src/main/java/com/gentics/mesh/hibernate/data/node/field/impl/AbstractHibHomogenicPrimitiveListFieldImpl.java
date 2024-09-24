package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.UUID;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * A subset of {@link AbstractHibListFieldImpl} for the homogenic primitive types, 
 * e.g. the item value type and item storage type match.
 * 
 * @author plyhun
 *
 * @param <I> a list item type, referenced from the <T>
 * @param <LF> a list field type, supported by this list container
 * @param <RM> a REST model representation of the <LF>
 * @param <U> an item field value type
 */
public abstract class AbstractHibHomogenicPrimitiveListFieldImpl<
			I extends AbstractHibListFieldEdgeImpl<U>, 
			LF extends HibListableField, 
			RM extends Field, 
			U
		> extends AbstractHibHeterogenicPrimitiveListFieldImpl<I, LF, RM, U, U> {

	public AbstractHibHomogenicPrimitiveListFieldImpl(UUID listUuid, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<I> itemClass) {
		super(listUuid, fieldKey, parent, itemClass);
	}

	public AbstractHibHomogenicPrimitiveListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<I> itemClass) {
		this(tx.uuidGenerator().generateType1UUID(), fieldKey, parent, itemClass);
	}
}

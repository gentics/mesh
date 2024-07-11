package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * A subset of {@link AbstractHibListFieldImpl} for the heterogenic primitive types, 
 * e.g. the item value type is a serializable subset of the item storage type.
 * 
 * @author plyhun
 *
 * @param <I> a list item type, referenced from the <T>
 * @param <LF> a list field type, supported by this list container
 * @param <RM> a REST model representation of the <LF>
 * @param <U> an item field value type
 * @param <V> an item entity type
 */
public abstract class AbstractHibHeterogenicPrimitiveListFieldImpl<
			I extends AbstractHibListFieldEdgeImpl<V>, 
			LF extends ListableField, 
			RM extends FieldModel, 
			U,
			V extends U
		> extends AbstractHibListFieldImpl<I, LF, RM, U, V> {

	public AbstractHibHeterogenicPrimitiveListFieldImpl(UUID listUuid, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<I> itemClass) {
		super(listUuid, fieldKey, parent, itemClass);
	}

	public AbstractHibHeterogenicPrimitiveListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Class<I> itemClass) {
		this(tx.uuidGenerator().generateType1UUID(), fieldKey, parent, itemClass);
	}

	/**
	 * BACKWARDS COMPATIBILITY: a wrapper around indexed primitive getter, assuming the index parameter starts from 1. 
	 * Don't use this method in API.
	 * 
	 * @param index
	 * @return
	 */
	@Deprecated
	public I get(int index) {
		return get(index-1, HibernateTx.get()).orElse(null);
	}

	public Optional<I> get(int index, HibernateTx tx) {
		return AbstractHibListFieldEdgeImpl.getItem(
					tx, itemClass, getContainer().getDbUuid(), getContainer().getReferenceType(), getFieldKey(), index);
	}

	protected I createItem(U value) {
		HibernateTx tx = HibernateTx.get();
		I item = makeFromValueAndIndex(value, getSize(), tx);
		return item;
	}

	protected void createItems(List<U> values) {
		HibernateTx tx = HibernateTx.get();
		makeFromValuesAndIndices(values, getSize(), tx);
	}
}

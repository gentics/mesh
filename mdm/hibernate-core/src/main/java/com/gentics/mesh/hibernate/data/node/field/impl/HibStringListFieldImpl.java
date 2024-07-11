package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibStringListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * String list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibStringListFieldImpl extends
			AbstractHibHomogenicPrimitiveListFieldImpl<HibStringListFieldEdgeImpl, HibStringField, StringFieldListImpl, String>
		implements HibStringFieldList {

	protected HibStringListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibStringListFieldEdgeImpl.class);
	}

	public HibStringListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibStringListFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getStringList, false);
		return HibStringListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public void createStrings(List<String> strings) {
		createItems(strings);
	}

	@Override
	public HibStringField createString(String value) {
		return createItem(value);
	}

	@Override
	public HibStringField getString(int index) {
		return get(index);
	}

	/**
	 * Make an edge for the given container, field key and values.
	 * This will also put the list items into the cache
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param values
	 * @return
	 */
	public static HibStringListFieldImpl fromContainer(HibernateTx tx,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, List<String> values) {
		HibStringListFieldImpl list = new HibStringListFieldImpl(tx, fieldKey, container);
		List<HibStringListFieldEdgeImpl> newItems = IntStream.range(0, values.size())
				.mapToObj(i -> new HibStringListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container)).collect(Collectors.toList());
		newItems.forEach(item -> tx.entityManager().persist(item));

		tx.data().getListableFieldCache().put(list.valueOrNull(), newItems);

		return list;
	}

	@Override
	protected String getValue(HibStringField field) {
		return field.getString();
	}

	@Override
	protected HibListFieldItemConstructor<HibStringListFieldEdgeImpl, String, String> getItemConstructor() {
		return HibStringListFieldEdgeImpl::new;
	}
}

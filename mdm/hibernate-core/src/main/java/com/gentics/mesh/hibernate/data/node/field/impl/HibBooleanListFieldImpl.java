package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBooleanListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Boolean list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibBooleanListFieldImpl 
			extends AbstractHibHomogenicPrimitiveListFieldImpl<HibBooleanListFieldEdgeImpl, HibBooleanField, BooleanFieldListImpl, Boolean> 
			implements HibBooleanFieldList {

	protected HibBooleanListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibBooleanListFieldEdgeImpl.class);
	}

	public HibBooleanListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibBooleanListFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getBooleanList, false);
		return HibBooleanListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public HibBooleanField createBoolean(Boolean value) {
		if (value == null) {
			return null;
		}
		return createItem(value);
	}

	@Override
	public void createBooleans(List<Boolean> items) {
		createItems(items.stream().filter(Objects::nonNull).collect(Collectors.toList()));
	}

	@Override
	public HibBooleanField getBoolean(int index) {
		return get(index);
	}

	/**
	 * Make an edge for the given container, field key and values.
	 * 
	 * @param tx
	 * @param container
	 * @param fieldKey
	 * @param values
	 * @return
	 */
	public static HibBooleanListFieldImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container,
			String fieldKey, List<Boolean> values) {
		HibBooleanListFieldImpl list = new HibBooleanListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
			.mapToObj(i -> new HibBooleanListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
			.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	protected Boolean getValue(HibBooleanField field) {
		return field.getBoolean();
	}

	@Override
	protected HibListFieldItemConstructor<HibBooleanListFieldEdgeImpl, Boolean, Boolean> getItemConstructor() {
		return HibBooleanListFieldEdgeImpl::new;
	}

}

package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibDateListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Date list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibDateListFieldImpl
		extends AbstractHibHomogenicPrimitiveListFieldImpl<HibDateListFieldEdgeImpl, DateField, DateFieldListImpl, Long>
		implements DateFieldList {

	protected HibDateListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibDateListFieldEdgeImpl.class);
	}

	public HibDateListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibDateListFieldEdgeImpl.class);
	}

	@Override
	public Field cloneTo(FieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getDateList, false);
		return HibDateListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public DateField createDate(Long value) {
		return createItem(value);
	}

	@Override
	public void createDates(List<Long> dates) {
		createItems(dates);
	}

	@Override
	public DateField getDate(int index) {
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
	public static HibDateListFieldImpl fromContainer(HibernateTx tx,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, List<Long> values) {
		HibDateListFieldImpl list = new HibDateListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
				.mapToObj(i -> new HibDateListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
				.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	protected Long getValue(DateField field) {
		return field.getDate();
	}

	@Override
	protected HibListFieldItemConstructor<HibDateListFieldEdgeImpl, Long, Long> getItemConstructor() {
		return HibDateListFieldEdgeImpl::new;
	}
}

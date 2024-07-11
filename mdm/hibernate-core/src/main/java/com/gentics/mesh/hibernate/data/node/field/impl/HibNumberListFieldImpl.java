package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibNumberListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Number list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibNumberListFieldImpl extends
		AbstractHibHeterogenicPrimitiveListFieldImpl<HibNumberListFieldEdgeImpl, HibNumberField, NumberFieldListImpl, Number, Double>
		implements HibNumberFieldList {

	protected HibNumberListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibNumberListFieldEdgeImpl.class);
	}

	public HibNumberListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibNumberListFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getNumberList, false);
		return HibNumberListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public HibNumberField createNumber(Number value) {
		return createItem(value);
	}

	@Override
	public void createNumbers(List<Number> items) {
		createItems(items);
	}

	@Override
	public HibNumberField getNumber(int index) {
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
	public static HibNumberListFieldImpl fromContainer(HibernateTx tx,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, List<Number> values) {
		HibNumberListFieldImpl list = new HibNumberListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
				.mapToObj(i -> new HibNumberListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
				.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	protected Number getValue(HibNumberField field) {
		return field.getNumber();
	}

	@Override
	protected HibListFieldItemConstructor<HibNumberListFieldEdgeImpl, Number, Double> getItemConstructor() {
		return HibNumberListFieldEdgeImpl::new;
	}
}

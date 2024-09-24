package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibHtmlListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * HTML list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibHtmlListFieldImpl
		extends AbstractHibHomogenicPrimitiveListFieldImpl<HibHtmlListFieldEdgeImpl, HibHtmlField, HtmlFieldListImpl, String>
		implements HibHtmlFieldList {

	protected HibHtmlListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibHtmlListFieldEdgeImpl.class);
	}

	public HibHtmlListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibHtmlListFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getHTMLList, false);
		return HibHtmlListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public HibHtmlField createHTML(String value) {
		return createItem(value);
	}

	@Override
	public void createHTMLs(List<String> items) {
		createItems(items);
	}

	@Override
	public HibHtmlField getHTML(int index) {
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
	public static HibHtmlListFieldImpl fromContainer(HibernateTx tx,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, List<String> values) {
		HibHtmlListFieldImpl list = new HibHtmlListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size())
				.mapToObj(i -> new HibHtmlListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
				.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	protected String getValue(HibHtmlField field) {
		return field.getHTML();
	}

	@Override
	protected HibListFieldItemConstructor<HibHtmlListFieldEdgeImpl, String, String> getItemConstructor() {
		return HibHtmlListFieldEdgeImpl::new;
	}
}

package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.data.node.field.list.HibJsonFieldList;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.list.impl.JsonFieldListImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibJsonListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

import io.vertx.core.json.JsonObject;

/**
 * JSON object list field implementation.
 * 
 * @author plyhun
 *
 */
public class HibJsonListFieldImpl extends
		AbstractHibHeterogenicPrimitiveListFieldImpl<HibJsonListFieldEdgeImpl, HibJsonField, JsonFieldListImpl, JsonObject, JsonObject>
		implements HibJsonFieldList {

	protected HibJsonListFieldImpl(HibernateTx tx, String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		super(tx, fieldKey, parent, HibJsonListFieldEdgeImpl.class);
	}

	public HibJsonListFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID initialValue) {
		super(initialValue, fieldKey, parent, HibJsonListFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> unmanagedBase = (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) container;
		unmanagedBase.ensureColumnExists(getFieldKey(), FieldTypes.LIST);
		unmanagedBase.ensureOldReferenceRemoved(tx, getFieldKey(), unmanagedBase::getJsonList, false);
		return HibJsonListFieldImpl.fromContainer(tx, unmanagedBase, getFieldKey(), getValues());
	}

	@Override
	public HibJsonField createJson(JsonObject value) {
		return createItem(value);
	}

	@Override
	public void createJsons(List<JsonObject> items) {
		createItems(items);
	}

	@Override
	public HibJsonField getJson(int index) {
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
	public static HibJsonListFieldImpl fromContainer(HibernateTx tx,
			HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, String fieldKey, List<JsonObject> values) {
		HibJsonListFieldImpl list = new HibJsonListFieldImpl(tx, fieldKey, container);
		IntStream.range(0, values.size()).mapToObj(
				i -> new HibJsonListFieldEdgeImpl(tx, list.valueOrNull(), i, fieldKey, values.get(i), container))
				.forEach(item -> tx.entityManager().persist(item));
		return list;
	}

	@Override
	protected JsonObject getValue(HibJsonField field) {
		return field.getJson();
	}

	@Override
	protected HibListFieldItemConstructor<HibJsonListFieldEdgeImpl, JsonObject, JsonObject> getItemConstructor() {
		return HibJsonListFieldEdgeImpl::new;
	}

}

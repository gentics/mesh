package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see DateGraphFieldList
 */
public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<HibDateField, DateFieldListImpl, Long> implements DateGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(DateGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibDateField createDate(Long date) {
		HibDateField field = createField();
		field.setDate(date);
		return field;
	}

	@Override
	protected DateGraphField createField(String key) {
		return new DateGraphFieldImpl(key, this);
	}

	@Override
	public HibDateField getDate(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends DateGraphField> getListType() {
		return DateGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	@Override
	public DateFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (HibDateField item : getList()) {
			restModel.add(toISO8601(item.getDate()));
		}
		return restModel;
	}

	@Override
	public List<Long> getValues() {
		return getList().stream().map(HibDateField::getDate).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DateFieldListImpl) {
			DateFieldListImpl restField = (DateFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HibDateField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> toISO8601(e.getDate())).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}

}

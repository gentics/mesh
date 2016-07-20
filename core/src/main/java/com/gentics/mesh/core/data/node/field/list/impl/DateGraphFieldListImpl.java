package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.CompareUtils;

import rx.Single;

/**
 * @see DateGraphFieldList
 */
public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<DateGraphField, DateFieldListImpl, Long> implements DateGraphFieldList {

	public static void init(Database database) {
		database.addVertexType(DateGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public DateGraphField createDate(Long date) {
		DateGraphField field = createField();
		field.setDate(date);
		return field;
	}

	@Override
	protected DateGraphField createField(String key) {
		return new DateGraphFieldImpl(key, getImpl());
	}

	@Override
	public DateGraphField getDate(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends DateGraphField> getListType() {
		return DateGraphFieldImpl.class;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		getElement().remove();
	}

	@Override
	public Single<DateFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (DateGraphField item : getList()) {
			restModel.add(item.getDate());
		}
		return Single.just(restModel);
	}

	@Override
	public List<Long> getValues() {
		return getList().stream().map(DateGraphField::getDate).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DateFieldListImpl) {
			DateFieldListImpl restField = (DateFieldListImpl) obj;
			List<Long> restList = restField.getItems();
			List<? extends DateGraphField> graphList = getList();
			List<Long> graphStringList = graphList.stream().map(e -> e.getDate()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}

}

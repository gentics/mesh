package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

import rx.Observable;

/**
 * @see DateGraphFieldList
 */
public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<DateGraphField, DateFieldListImpl, Long>implements DateGraphFieldList {

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
	public Observable<DateFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (DateGraphField item : getList()) {
			restModel.add(item.getDate());
		}
		return Observable.just(restModel);
	}

	@Override
	public List<Long> getValues() {
		return getList().stream().map(DateGraphField::getDate).collect(Collectors.toList());
	}
}

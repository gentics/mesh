package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * @see NumberGraphFieldList
 */
public class NumberGraphFieldListImpl extends AbstractBasicGraphFieldList<NumberGraphField, NumberFieldListImpl, Number> implements NumberGraphFieldList {

	@Override
	public NumberGraphField createNumber(Number number) {
		NumberGraphField field = createField();
		field.setNumber(number);
		return field;
	}

	@Override
	public NumberGraphField getNumber(int index) {
		return getField(index);
	}

	@Override
	protected NumberGraphField createField(String key) {
		return new NumberGraphFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends NumberGraphField> getListType() {
		return NumberGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public Observable<NumberFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags) {
		NumberFieldListImpl restModel = new NumberFieldListImpl();
		for (NumberGraphField item : getList()) {
			restModel.add(item.getNumber());
		}
		return Observable.just(restModel);
	}

	@Override
	public List<Number> getValues() {
		return getList().stream().map(NumberGraphField::getNumber).collect(Collectors.toList());
	}
}

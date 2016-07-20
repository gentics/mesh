package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.CompareUtils;

import rx.Single;

/**
 * @see NumberGraphFieldList
 */
public class NumberGraphFieldListImpl extends AbstractBasicGraphFieldList<NumberGraphField, NumberFieldListImpl, Number>
		implements NumberGraphFieldList {

	public static void init(Database database) {
		database.addVertexType(NumberGraphFieldListImpl.class, MeshVertexImpl.class);
	}

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
	public void delete(SearchQueueBatch batch) {
		getElement().remove();
	}

	@Override
	public Single<NumberFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		NumberFieldListImpl restModel = new NumberFieldListImpl();
		for (NumberGraphField item : getList()) {
			restModel.add(item.getNumber());
		}
		return Single.just(restModel);
	}

	@Override
	public List<Number> getValues() {
		return getList().stream().map(NumberGraphField::getNumber).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumberFieldListImpl) {
			NumberFieldListImpl restField = (NumberFieldListImpl) obj;
			List<Number> restList = restField.getItems();
			List<? extends NumberGraphField> graphList = getList();
			List<Number> graphStringList = graphList.stream().map(e -> e.getNumber()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return super.equals(obj);
	}
}

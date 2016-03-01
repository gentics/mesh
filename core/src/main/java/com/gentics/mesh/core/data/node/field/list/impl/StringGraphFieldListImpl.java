package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;

import rx.Observable;

/**
 * @see StringGraphFieldList
 */
public class StringGraphFieldListImpl extends AbstractBasicGraphFieldList<StringGraphField, StringFieldListImpl, String> implements StringGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(StringGraphFieldListImpl.class);
	}

	@Override
	public StringGraphField createString(String string) {
		StringGraphField field = createField();
		field.setString(string);
		return field;
	}

	@Override
	public StringGraphField getString(int index) {
		return getField(index);
	}

	@Override
	protected StringGraphField createField(String key) {
		return new StringGraphFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends StringGraphField> getListType() {
		return StringGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public Observable<StringFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags) {
		StringFieldListImpl restModel = new StringFieldListImpl();
		for (StringGraphField item : getList()) {
			restModel.add(item.getString());
		}
		return Observable.just(restModel);
	}

	@Override
	public List<String> getValues() {
		return getList().stream().map(StringGraphField::getString).collect(Collectors.toList());
	}
}

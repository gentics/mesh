package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.StringField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface StringFieldList extends MicroschemaListableField, ListField<StringField, StringFieldListImpl, String> {

	String TYPE = "string";

	/**
	 * Create a new string field and add it to the list.
	 * 
	 * @param string
	 * @return
	 */
	StringField createString(String string);

	/**
	 * Create an ordered list of string fields from the values, adding all to the list.
	 * 
	 * @param strings
	 */
	default void createStrings(List<String> strings) {
		strings.stream().forEach(this::createString);
	}

	/**
	 * Return the string item at the given position.
	 * 
	 * @param index
	 * @return
	 */
	StringField getString(int index);

	@Override
	default StringFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		StringFieldListImpl restModel = new StringFieldListImpl();
		for (StringField item : getList()) {
			restModel.add(item.getString());
		}
		return restModel;
	}

	@Override
	default List<String> getValues() {
		return getList().stream().map(StringField::getString).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof StringFieldListImpl) {
			StringFieldListImpl restField = (StringFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends StringField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getString()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return ListField.super.listEquals(obj);
	}
}

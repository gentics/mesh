package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface HibStringFieldList extends HibMicroschemaListableField, HibListField<HibStringField, StringFieldListImpl, String> {

	String TYPE = "string";

	/**
	 * Create a new string field and add it to the list.
	 * 
	 * @param string
	 * @return
	 */
	HibStringField createString(String string);

	/**
	 * Return the string item at the given position.
	 * 
	 * @param index
	 * @return
	 */
	HibStringField getString(int index);

	@Override
	default StringFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		StringFieldListImpl restModel = new StringFieldListImpl();
		for (HibStringField item : getList()) {
			restModel.add(item.getString());
		}
		return restModel;
	}

	@Override
	default List<String> getValues() {
		return getList().stream().map(HibStringField::getString).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof StringFieldListImpl) {
			StringFieldListImpl restField = (StringFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HibStringField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> e.getString()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return HibListField.super.listEquals(obj);
	}
}

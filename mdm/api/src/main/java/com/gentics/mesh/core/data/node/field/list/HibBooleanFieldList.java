package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface HibBooleanFieldList extends HibMicroschemaListableField, HibListField<HibBooleanField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	HibBooleanField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	HibBooleanField createBoolean(Boolean flag);

	@Override
	default BooleanFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		BooleanFieldListImpl restModel = new BooleanFieldListImpl();
		for (HibBooleanField item : getList()) {
			restModel.add(item.getBoolean());
		}
		return restModel;
	}

	@Override
	default List<Boolean> getValues() {
		return getList().stream().map(HibBooleanField::getBoolean).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof BooleanFieldListImpl) {
			BooleanFieldListImpl restField = (BooleanFieldListImpl) obj;
			List<Boolean> restList = restField.getItems();
			List<? extends HibBooleanField> graphList = getList();
			List<Boolean> graphStringList = graphList.stream().map(e -> e.getBoolean()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return HibListField.super.listEquals(obj);
	}
}

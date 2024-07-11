package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.BooleanField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface BooleanFieldList extends MicroschemaListableField, ListField<BooleanField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	BooleanField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	BooleanField createBoolean(Boolean flag);

	/**
	 * Create an ordered list of boolean items, adding all to the list.
	 * 
	 * @param items
	 */
	default void createBooleans(List<Boolean> items) {
		items.stream().forEach(this::createBoolean);
	}

	@Override
	default BooleanFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		BooleanFieldListImpl restModel = new BooleanFieldListImpl();
		for (BooleanField item : getList()) {
			restModel.add(item.getBoolean());
		}
		return restModel;
	}

	@Override
	default List<Boolean> getValues() {
		return getList().stream().map(BooleanField::getBoolean).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof BooleanFieldListImpl) {
			BooleanFieldListImpl restField = (BooleanFieldListImpl) obj;
			List<Boolean> restList = restField.getItems();
			List<? extends BooleanField> graphList = getList();
			List<Boolean> graphStringList = graphList.stream().map(e -> e.getBoolean()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return ListField.super.listEquals(obj);
	}
}

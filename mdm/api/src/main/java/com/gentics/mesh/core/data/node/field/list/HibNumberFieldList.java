package com.gentics.mesh.core.data.node.field.list;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.util.CompareUtils;
import com.gentics.mesh.util.NumberUtils;

public interface HibNumberFieldList extends HibMicroschemaListableField, HibListField<HibNumberField, NumberFieldListImpl, Number> {
	String TYPE = "number";

	/**
	 * Create a new number graph field with the given value.
	 * 
	 * @param value
	 * @return
	 */
	HibNumberField createNumber(Number value);

	/**
	 * Return the graph number field at the given position.
	 * 
	 * @param index
	 * @return
	 */
	HibNumberField getNumber(int index);

	@Override
	default NumberFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		NumberFieldListImpl restModel = new NumberFieldListImpl();
		for (HibNumberField item : getList()) {
			restModel.add(item.getNumber());
		}
		return restModel;
	}

	@Override
	default List<Number> getValues() {
		return getList().stream().map(HibNumberField::getNumber).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof NumberFieldListImpl) {
			NumberFieldListImpl restField = (NumberFieldListImpl) obj;
			List<Number> restList = restField.getItems();
			List<? extends HibNumberField> graphList = getList();
			List<Number> graphStringList = graphList.stream().map(e -> e.getNumber()).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList, Optional.of((o1, o2) -> NumberUtils.compare(o1, o2) == 0));
		}
		return HibListField.super.listEquals(obj);
	}
}

package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface HibDateFieldList extends HibMicroschemaListableField, HibListField<HibDateField, DateFieldListImpl, Long> {

	String TYPE = "date";

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param date
	 *            Date to be set for the new field
	 * @return
	 */
	HibDateField createDate(Long date);

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	HibDateField getDate(int index);

	@Override
	default DateFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (HibDateField item : getList()) {
			restModel.add(toISO8601(item.getDate()));
		}
		return restModel;
	}

	@Override
	default List<Long> getValues() {
		return getList().stream().map(HibDateField::getDate).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof DateFieldListImpl) {
			DateFieldListImpl restField = (DateFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends HibDateField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> toISO8601(e.getDate())).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return HibListField.super.listEquals(obj);
	}
}

package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.util.CompareUtils;

public interface DateFieldList extends MicroschemaListableField, ListField<DateField, DateFieldListImpl, Long> {

	String TYPE = "date";

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param date
	 *            Date to be set for the new field
	 * @return
	 */
	DateField createDate(Long date);

	/**
	 * Create an ordered list of date fields from values, adding all to the list.
	 * 
	 * @param dates
	 */
	default void createDates(List<Long> dates) {
		dates.stream().forEach(this::createDate);
	}

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	DateField getDate(int index);

	@Override
	default DateFieldListImpl transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (DateField item : getList()) {
			restModel.add(toISO8601(item.getDate()));
		}
		return restModel;
	}

	@Override
	default List<Long> getValues() {
		return getList().stream().map(DateField::getDate).collect(Collectors.toList());
	}

	@Override
	default boolean listEquals(Object obj) {
		if (obj instanceof DateFieldListImpl) {
			DateFieldListImpl restField = (DateFieldListImpl) obj;
			List<String> restList = restField.getItems();
			List<? extends DateField> graphList = getList();
			List<String> graphStringList = graphList.stream().map(e -> toISO8601(e.getDate())).collect(Collectors.toList());
			return CompareUtils.equals(restList, graphStringList);
		}
		return ListField.super.listEquals(obj);
	}
}

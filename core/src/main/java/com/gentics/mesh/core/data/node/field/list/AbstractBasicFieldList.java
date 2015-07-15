package com.gentics.mesh.core.data.node.field.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public abstract class AbstractBasicFieldList<T extends ListableField> extends AbstractFieldList<T> {

	protected abstract T convertBasicValue(String listItemValue);

	@Override
	public List<? extends T> getList() {
		Map<String, String> map = getProperties("item");
		List<T> list = new ArrayList<>();
		for (String itemValue : map.values()) {
			list.add(convertBasicValue(itemValue));
		}
		return list;
	}
}

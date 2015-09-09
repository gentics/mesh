package com.gentics.mesh.core.data.node.field.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractBasicGraphFieldList<T extends ListableGraphField, RM extends Field> extends AbstractGraphFieldList<T, RM> {

	protected abstract T createField(String key);

	protected T convertBasicValue(String itemKey) {
		String key = itemKey.substring(0, itemKey.lastIndexOf("-"));
		return createField(key);
	}

	protected T getField(int index) {
		return createField("item-" + index);
	}

	protected T createField() {
		return createField("item-" + (getSize() + 1));
	}

	@Override
	public long getSize() {
		return getProperties("item").size();
	}

	@Override
	public void removeAll() {
		for (String key : getProperties("item-").keySet()) {
			setProperty(key, null);
		}
	}

	@Override
	public List<? extends T> getList() {

		Map<String, String> map = getProperties("item");
		List<T> list = new ArrayList<>();
		for (String itemKey : map.keySet()) {
			list.add(convertBasicValue(itemKey));
		}
		return list;
	}
}

package com.gentics.mesh.core.data.node.field.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractBasicGraphFieldList<T extends ListableGraphField, RM extends Field, U> extends AbstractGraphFieldList<T, RM, U> {

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
		// TODO sorting is not very efficient, because the keys are transformed to their order too often
		map.keySet().stream().sorted((key1, key2) -> {
			int index1 = Integer.parseInt(key1.substring("item-".length(), key1.lastIndexOf("-")));
			int index2 = Integer.parseInt(key2.substring("item-".length(), key2.lastIndexOf("-")));
			return index1 - index2;
		}).forEachOrdered(itemKey -> {
			list.add(convertBasicValue(itemKey));
		});
		return list;
	}

	@Override
	public void removeField() {
		delete(null);
	}
}

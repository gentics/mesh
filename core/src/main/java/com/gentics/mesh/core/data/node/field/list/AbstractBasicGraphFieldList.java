package com.gentics.mesh.core.data.node.field.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public abstract class AbstractBasicGraphFieldList<T extends ListableGraphField> extends AbstractGraphFieldList<T> {

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
	public List<? extends T> getList() {
//		for (String key : getPropertyKeys()) {
//			log.debug(key + "=" + getProperty(key));
//		}
		Map<String, String> map = getProperties("item");
		List<T> list = new ArrayList<>();
		for (String itemKey : map.keySet()) {
			list.add(convertBasicValue(itemKey));
		}
		return list;
	}
}

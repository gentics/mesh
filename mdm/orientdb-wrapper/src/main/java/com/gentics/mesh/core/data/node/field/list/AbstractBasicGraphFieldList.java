package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.util.CompareUtils;

/**
 * Abstract class for basic graph field lists. Basic graph field lists are stored within dedicated vertices. The values of such lists are stored as properties
 * within the vertex that represents the list.
 *
 * @param <T>
 *            Field type that represents a list item
 * @param <RM>
 *            Rest model type of the list
 * @param <U>
 *            Value type that is stored in the list
 */
public abstract class AbstractBasicGraphFieldList<T extends HibListableField, RM extends Field, U> extends AbstractGraphFieldList<T, RM, U> {

	/**
	 * Create a new field wrapper which is used to handle the field value.
	 * 
	 * @param key
	 * @return
	 */
	protected abstract T createField(String key);

	protected T convertBasicValue(String itemKey) {
		String key = itemKey.substring(0, itemKey.lastIndexOf("-"));
		return createField(key);
	}

	/**
	 * Load the field for the given index.
	 * 
	 * @param index
	 * @return
	 */
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
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		toGraph(container).unlinkOut(this, HAS_LIST);

		if (!in(HAS_LIST).hasNext()) {
			delete(bac);
		}
	}

	@Override
	public GraphField cloneTo(HibFieldContainer container) {
		GraphFieldContainer graphContainer = toGraph(container);
		graphContainer.linkOut(this, HAS_LIST);
		return graphContainer.getList(getClass(), getFieldKey());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ListGraphField) {
			List<? extends T> listA = getList();
			List<? extends T> listB = ((ListGraphField) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}

	/**
	 * Delete the vertex which is used to store the field. This will effectively also delete the field.
	 */
	public void removeField() {
		delete();
	}
}

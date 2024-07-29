package com.gentics.mesh.core.rest.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.json.JsonUtil;

/**
 * Abstract class for various field lists.
 * 
 * @param <T>
 *            Field list value type
 */
public abstract class AbstractFieldList<T> implements FieldList<T> {

	private List<T> items = new ArrayList<>();

	//	private long totalCount;

	@Override
	public List<T> getItems() {
		return items;
	}

	@Override
	public FieldList<T> setItems(List<T> items) {
		this.items = items;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return "list";
	}

	@Override
	public void add(T field) {
		items.add(field);
	}

	//	@Override
	//	public long getTotalCount() {
	//		return totalCount;
	//	}
	//
	//	@Override
	//	public void setTotalCount(long totalCount) {
	//		this.totalCount = totalCount;
	//	}

	@Override
	public String toString() {
		return JsonUtil.toJson(getItems(), true);
	}
}

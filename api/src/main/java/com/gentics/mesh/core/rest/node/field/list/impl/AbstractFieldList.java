package com.gentics.mesh.core.rest.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractFieldList<T> implements Field {

	private List<T> list = new ArrayList<>();

	private String order;

	private String orderBy;

	private long totalCount;

	public List<T> getList() {
		return list;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return "list";
	}

	public void add(T field) {
		list.add(field);
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
}

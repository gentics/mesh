package com.gentics.mesh.core.rest.search;

public class EntityMetrics {
	private TypeMetrics insert = new TypeMetrics();
	private TypeMetrics update = new TypeMetrics();
	private TypeMetrics delete = new TypeMetrics();

	public TypeMetrics getInsert() {
		return insert;
	}

	public EntityMetrics setInsert(TypeMetrics insert) {
		this.insert = insert;
		return this;
	}

	public TypeMetrics getUpdate() {
		return update;
	}

	public EntityMetrics setUpdate(TypeMetrics update) {
		this.update = update;
		return this;
	}

	public TypeMetrics getDelete() {
		return delete;
	}

	public EntityMetrics setDelete(TypeMetrics delete) {
		this.delete = delete;
		return this;
	}
}

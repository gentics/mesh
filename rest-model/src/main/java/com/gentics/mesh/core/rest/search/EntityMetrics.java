package com.gentics.mesh.core.rest.search;

import com.gentics.mesh.annotation.Setter;

/**
 * POJO to store metric information of a specific entity type. (Used during index sync)
 */
public class EntityMetrics {

	private TypeMetrics insert = new TypeMetrics();
	private TypeMetrics update = new TypeMetrics();
	private TypeMetrics delete = new TypeMetrics();

	public TypeMetrics getInsert() {
		return insert;
	}

	@Setter
	public EntityMetrics setInsert(TypeMetrics insert) {
		this.insert = insert;
		return this;
	}

	public TypeMetrics getUpdate() {
		return update;
	}

	@Setter
	public EntityMetrics setUpdate(TypeMetrics update) {
		this.update = update;
		return this;
	}

	public TypeMetrics getDelete() {
		return delete;
	}

	@Setter
	public EntityMetrics setDelete(TypeMetrics delete) {
		this.delete = delete;
		return this;
	}
}

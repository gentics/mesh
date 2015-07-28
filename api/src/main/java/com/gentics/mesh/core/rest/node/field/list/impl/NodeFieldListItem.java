package com.gentics.mesh.core.rest.node.field.list.impl;

public class NodeFieldListItem {

	private String uuid;

	public NodeFieldListItem() {
	}

	public NodeFieldListItem(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

	public NodeFieldListItem setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

}

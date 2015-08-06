package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;

public class NodeFieldListItemImpl implements NodeFieldListItem {

	private String uuid;

	public NodeFieldListItemImpl() {
	}

	public NodeFieldListItemImpl(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	public NodeFieldListItemImpl setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

}

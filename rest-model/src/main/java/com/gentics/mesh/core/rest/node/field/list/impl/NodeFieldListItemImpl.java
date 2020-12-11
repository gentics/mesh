package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;

/**
 * REST model for a node list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class NodeFieldListItemImpl implements NodeFieldListItem {

	private String uuid;

	private String url;

	public NodeFieldListItemImpl() {
	}

	public NodeFieldListItemImpl(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the node item.
	 * 
	 * @param uuid
	 * @return
	 */
	public NodeFieldListItemImpl setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public String getPath() {
		return url;
	}

	/**
	 * Set the webroot URL
	 * 
	 * @param url webroot URL
	 * @return this instance
	 */
	public NodeFieldListItemImpl setUrl(String url) {
		this.url = url;
		return this;
	}
}

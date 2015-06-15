package com.gentics.mesh.core.rest.node.request;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;

public class NodeUpdateRequest extends AbstractRestModel {

	private Map<String, String> properties = new HashMap<>();

	private SchemaReference schema;

	private long order = 0;

	public NodeUpdateRequest() {
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the property for the given key.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

}

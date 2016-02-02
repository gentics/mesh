package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

public class SchemaChangeModelImpl implements RestModel {

	private String uuid;

	private SchemaChangeOperation operation;

	private Map<String, Object> properties = new HashMap<>();

	/**
	 * Return the uuid of the change.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the change.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the operation for the change.
	 * 
	 * @return
	 */
	public SchemaChangeOperation getOperation() {
		return operation;
	}

	/**
	 * Set the operation for this change.
	 * 
	 * @param operation
	 */
	public SchemaChangeModelImpl setOperation(SchemaChangeOperation operation) {
		this.operation = operation;
		return this;
	}

	/**
	 * Return additional properties for the change.
	 * 
	 * @return
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

}

package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

public class SchemaChangeModelImpl implements RestModel {

	private String uuid;

	private String operation;

	private String field;

	private Map<String, String> properties = new HashMap<>();

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
	public String getOperation() {
		return operation;
	}

	/**
	 * Set the operation for this change.
	 * 
	 * @param operation
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * Return additional properties for the change.
	 * 
	 * @return
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the key of the field on which the operation will be performed.
	 * 
	 * @return
	 */
	public String getField() {
		return field;
	}

	/**
	 * Set the key of the field on which the operation will be performed.
	 * 
	 * @param field
	 */
	public void setField(String field) {
		this.field = field;
	}

}

package com.gentics.mesh.core.rest.schema.change.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a schema change.
 */
public class SchemaChangeModel implements RestModel {

	private static final String FIELD_NAME_KEY = "field";

	private static final String REQUIRED_KEY = "required";

	private String uuid;

	private SchemaChangeOperation operation;

	private String migrationScript;

	private Map<String, Object> properties = new HashMap<>();

	public SchemaChangeModel() {
	}

	/**
	 * Create a new change that includes field name information.
	 * 
	 * @param operation
	 * @param fieldName
	 */
	public SchemaChangeModel(SchemaChangeOperation operation, String fieldName) {
		this(operation);
		getProperties().put(FIELD_NAME_KEY, fieldName);
	}

	/**
	 * Create a new change.
	 * 
	 * @param operation
	 */
	public SchemaChangeModel(SchemaChangeOperation operation) {
		this.operation = operation;
	}

	@JsonIgnore
	public String getFieldName() {
		return (String) getProperties().get(FIELD_NAME_KEY);
	}

	/**
	 * Return the UUID of the change.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the UUID of the change.
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
	public SchemaChangeModel setOperation(SchemaChangeOperation operation) {
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

	/**
	 * Set the required property for the change. This indicates that the required flag was changed to the given value for this change.
	 * 
	 * @param flag
	 */
	public void setRequired(boolean flag) {
		getProperties().put(REQUIRED_KEY, flag);
	}

	/**
	 * Return the required property for the change.
	 * 
	 * @return
	 */
	public boolean getRequired() {
		return Boolean.valueOf(String.valueOf(getProperties().get(REQUIRED_KEY)));
	}

	/**
	 * Return the custom migration script.
	 * 
	 * @return
	 */
	public String getMigrationScript() {
		return migrationScript;
	}

	/**
	 * Set the custom migration script.
	 * 
	 * @param migrationScript
	 */
	public void setMigrationScript(String migrationScript) {
		this.migrationScript = migrationScript;
	}

	/**
	 * Load the default (auto) migration script for the operation of the change.
	 * 
	 * @throws IOException
	 */
	public void loadMigrationScript() throws IOException {
		setMigrationScript(getOperation().getAutoMigrationScript(getProperties()));
	}

}

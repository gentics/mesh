package com.gentics.mesh.core.rest.schema.change.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.CHANGEFIELDTYPE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEMICROSCHEMA;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a schema change.
 */
public class SchemaChangeModel implements RestModel {

	public static final String FIELD_NAME_KEY = "field";

	public static final String ADD_FIELD_AFTER_KEY = "after";

	public static final String REQUIRED_KEY = "required";

	public static final String ELASTICSEARCH_KEY = "elasticsearch";

	public static final String SEGMENT_FIELD_KEY = "segmentFieldname";

	public static final String URLFIELDS_KEY = "urlFieldsname";

	public static final String DISPLAY_FIELD_NAME_KEY = "displayFieldname";
	
	public static final String VERSIONED_FLAG_KEY = "versioned";

	public static final String FIELD_ORDER_KEY = "order";

	public static final String NAME_KEY = "name";

	public static final String DESCRIPTION_KEY = "description";

	public static final String LABEL_KEY = "label";

	public static final String ALLOW_KEY = "allow";

	public static final String TYPE_KEY = "type";

	public static final String LIST_TYPE_KEY = "listType";

	public static final String CONTAINER_FLAG_KEY = "container";

	@JsonPropertyDescription("Uuid of the change entry")
	private String uuid;

	@JsonPropertyDescription("Type of operation for this change")
	private SchemaChangeOperation operation;

	@JsonPropertyDescription("Optional migation script")
	private String migrationScript;

	private Map<String, Object> properties = new HashMap<>();

	public SchemaChangeModel() {
	}

	/**
	 * Create a new change.
	 * 
	 * @param operation
	 */
	private SchemaChangeModel(SchemaChangeOperation operation) {
		this.operation = operation;
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
	 * Set an additional property to the change.
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, Object value) {
		properties.put(key, value);
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
	 */
	public void loadMigrationScript() {
		setMigrationScript(getOperation().getAutoMigrationScript(getProperties()));
	}

	/**
	 * Create a new update schema change.
	 * 
	 * @return
	 */
	public static SchemaChangeModel createUpdateSchemaChange() {
		return new SchemaChangeModel(UPDATESCHEMA);
	}

	/**
	 * Create a new update microschema change.
	 * 
	 * @return
	 */
	public static SchemaChangeModel createUpdateMicroschemaChange() {
		return new SchemaChangeModel(UPDATEMICROSCHEMA);
	}

	/**
	 * Create a new field removal change.
	 * 
	 * @param name
	 * @return
	 */
	public static SchemaChangeModel createRemoveFieldChange(String name) {
		return new SchemaChangeModel(REMOVEFIELD, name);
	}

	/**
	 * Create a change field type change.
	 * 
	 * @param fieldName
	 * @param type
	 * @return
	 */
	public static SchemaChangeModel createChangeFieldTypeChange(String fieldName, String type) {
		SchemaChangeModel change = new SchemaChangeModel(CHANGEFIELDTYPE, fieldName);
		change.getProperties().put(SchemaChangeModel.TYPE_KEY, type);
		return change;
	}

	/**
	 * Create a add field change.
	 * 
	 * @param fieldName
	 *            Field key
	 * @param type
	 *            Field type
	 * @param label
	 *            Field label
	 * @return
	 */
	public static SchemaChangeModel createAddFieldChange(String fieldName, String type, String label) {
		SchemaChangeModel change = new SchemaChangeModel(ADDFIELD, fieldName);
		change.getProperties().put(SchemaChangeModel.TYPE_KEY, type);
		change.getProperties().put(SchemaChangeModel.LABEL_KEY, label);
		return change;
	}

	@Override
	public String toString() {
		return getOperation() + ":" + getUuid() + ":" + getProperties();
	}

	/**
	 * Return the model property of the given key.
	 * 
	 * @param key
	 * @return
	 */
	public <T> T getProperty(String key) {
		return (T) properties.get(key);
	}

}

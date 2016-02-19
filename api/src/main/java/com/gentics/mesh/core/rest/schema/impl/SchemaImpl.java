package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

public class SchemaImpl extends AbstractFieldSchemaContainer implements Schema {

	private String displayField;
	private String segmentField;
	private boolean container = false;

	/**
	 * Create a new schema with the given name.
	 * 
	 * @param name
	 */
	public SchemaImpl(String name) {
		super(name);
	}

	public SchemaImpl() {
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	@Override
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public void setSegmentField(String segmentField) {
		this.segmentField = segmentField;
	}

	@Override
	public boolean isContainer() {
		return container;
	}

	@Override
	public void setContainer(boolean flag) {
		this.container = flag;
	}

	@Override
	public void validate() {
		super.validate();
		// TODO make sure that the display name field only maps to string fields since NodeImpl can currently only deal with string field values for
		// displayNames
		if (getFields().isEmpty()) {
			throw error(BAD_REQUEST, "schema_error_no_fields");
		}

		if (getDisplayField() == null) {
			throw error(BAD_REQUEST, "schema_error_displayfield_not_set");
		}

		if (!getFields().stream().map(FieldSchema::getName).anyMatch(e -> e.equals(getDisplayField()))) {
			throw error(BAD_REQUEST, "schema_error_displayfield_invalid", getDisplayField());
		}

		if (!(getField(getDisplayField()) instanceof StringFieldSchema)) {
			throw error(BAD_REQUEST, "schema_error_displayfield_type_invalid", getDisplayField());
		}

		if (getSegmentField() != null && !getFields().stream().map(FieldSchema::getName).anyMatch(e -> e.equals(getSegmentField()))) {
			throw error(BAD_REQUEST, "schema_error_segmentfield_invalid", getSegmentField());
		}

		//TODO make sure that segment fields are set to mandatory.

	}

}

package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.MeshJsonException;

public class SchemaImpl extends AbstractFieldSchemaContainer implements Schema {

	private String displayField;
	private String segmentField;
	private boolean container = false;

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
	public void validate() throws MeshJsonException {
		super.validate();
		// TODO make sure that the display name field only maps to string fields since NodeImpl can currently only deal with string field values for
		// displayNames

		if (getDisplayField() == null) {
			throw new MeshJsonException("The displayField property must be set.");
		}
		if (getFields().contains(getDisplayField())) {
			throw new MeshJsonException("The displayField value {" + getSegmentField() + "} does not match any fields");
		}

		if (getSegmentField() == null) {
			throw new MeshJsonException("The segmentField property must be set.");
		}
		if (getFields().contains(getSegmentField())) {
			throw new MeshJsonException("The segmentField value {" + getSegmentField() + "} does not match any fields");
		}

		if (getFields().isEmpty()) {
			throw new MeshJsonException("The schema must have at least one field.");
		}

		//TODO make sure that segment fields are set to mandatory.

	}

}

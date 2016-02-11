package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;

import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * @see UpdateSchemaChange
 */
public class UpdateSchemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Schema> implements UpdateSchemaChange {

	@Override
	public Schema apply(Schema schema) {
		schema = super.apply(schema);

		String displayFieldname = getDisplayField();
		if (displayFieldname != null) {
			schema.setDisplayField(displayFieldname);
		}

		String segmentFieldname = getSegmentField();
		if (segmentFieldname != null) {
			schema.setSegmentField(segmentFieldname);
		}

		Boolean containerFlag = getContainerFlag();
		if (containerFlag != null) {
			schema.setContainer(containerFlag);
		}

		return schema;
	}

	@Override
	public void setDisplayField(String fieldName) {
		setProperty(DISPLAY_FIELD_NAME_KEY, fieldName);
	}

	@Override
	public String getDisplayField() {
		return getProperty(DISPLAY_FIELD_NAME_KEY);
	}

	@Override
	public void setContainerFlag(Boolean flag) {
		setProperty(CONTAINER_FIELD_KEY, flag);
	}

	@Override
	public Boolean getContainerFlag() {
		return getProperty(CONTAINER_FIELD_KEY);
	}

	@Override
	public void setSegmentField(String fieldName) {
		setProperty(SEGMENT_FIELD_KEY, fieldName);
	}

	@Override
	public String getSegmentField() {
		return getProperty(SEGMENT_FIELD_KEY);
	}

}

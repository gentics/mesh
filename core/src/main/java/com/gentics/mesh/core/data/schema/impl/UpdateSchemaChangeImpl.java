package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see UpdateSchemaChange
 */
public class UpdateSchemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Schema> implements UpdateSchemaChange {

	public static void checkIndices(Database database) {
		database.addVertexType(UpdateSchemaChangeImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public <R extends FieldSchemaContainer> R apply(R container) {
		if (!(container instanceof Schema)) {
			throw error(BAD_REQUEST, "The provided container was no " + Schema.class.getName() + " got {" + container.getClass().getName() + "}");
		}

		Schema schema = (Schema) super.apply(container);

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

		return (R) schema;
	}

	@Override
	public void setDisplayField(String fieldName) {
		setRestProperty(DISPLAY_FIELD_NAME_KEY, fieldName);
	}

	@Override
	public String getDisplayField() {
		return getRestProperty(DISPLAY_FIELD_NAME_KEY);
	}

	@Override
	public void setContainerFlag(Boolean flag) {
		setRestProperty(CONTAINER_FLAG_KEY, flag);
	}

	@Override
	public Boolean getContainerFlag() {
		return getRestProperty(CONTAINER_FLAG_KEY);
	}

	@Override
	public void setSegmentField(String fieldName) {
		setRestProperty(SEGMENT_FIELD_KEY, fieldName);
	}

	@Override
	public String getSegmentField() {
		return getRestProperty(SEGMENT_FIELD_KEY);
	}

}

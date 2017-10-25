package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.URLFIELDS_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see UpdateSchemaChange
 */
public class UpdateSchemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Schema> implements UpdateSchemaChange {

	public static void init(Database database) {
		database.addVertexType(UpdateSchemaChangeImpl.class, MeshVertexImpl.class);
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

		List<String> urlFields = getURLFields();
		if (urlFields != null) {
			schema.setUrlFields(urlFields);
		}

		// We handle empty string as null
		if (segmentFieldname != null && isEmpty(segmentFieldname)) {
			schema.setSegmentField(null);
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

	@Override
	public List<String> getURLFields() {
		Object[] value = getRestProperty(URLFIELDS_KEY);
		if (value == null) {
			return new ArrayList<>();
		}
		String[] stringArray = Arrays.copyOf(value, value.length, String[].class);
		return Arrays.asList(stringArray);
	}

	@Override
	public void setURLFields(String... keys) {
		setRestProperty(URLFIELDS_KEY, keys);
	}

	@Override
	public void updateFromRest(SchemaChangeModel restChange) {
		/***
		 * Many graph databases can't handle null values. Tinkerpop blueprint contains constrains which avoid setting null values. We store empty string for the
		 * segment field name instead. It is possible to set setStandardElementConstraints for each tx to false in order to avoid such checks.
		 */
		if (restChange.getProperties().containsKey(SEGMENT_FIELD_KEY) && restChange.getProperty(SEGMENT_FIELD_KEY) == null) {
			restChange.setProperty(SEGMENT_FIELD_KEY, "");
		}
		super.updateFromRest(restChange);
	}

}

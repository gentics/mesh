package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.AUTO_PURGE_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.URLFIELDS_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.json.JsonObject;

/**
 * Change entry that contains information on how to change schema specific attributes.
 */
public interface HibUpdateSchemaChange extends HibFieldSchemaContainerUpdateChange<SchemaModel> {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATESCHEMA;

	/**
	 * Set the displayField name.
	 * 
	 * @param fieldName
	 */
	default void setDisplayField(String fieldName) {
		setRestProperty(DISPLAY_FIELD_NAME_KEY, fieldName);
	}

	/**
	 * Return the displayField name.
	 * 
	 * @return
	 */
	default String getDisplayField() {
		return getRestProperty(DISPLAY_FIELD_NAME_KEY);
	}

	/**
	 * Set the container flag.
	 * 
	 * @param flag
	 */
	default void setContainerFlag(Boolean flag) {
		setRestProperty(CONTAINER_FLAG_KEY, flag);
	}

	/**
	 * Return the container flag.
	 * 
	 * @return
	 */
	default Boolean getContainerFlag() {
		return getRestProperty(CONTAINER_FLAG_KEY);
	}

	/**
	 * Return the auto purge flag.
	 * 
	 * @return
	 */
	default Boolean getAutoPurgeFlag() {
		return getRestProperty(AUTO_PURGE_FLAG_KEY);
	}

	/**
	 * Set the auto purge flag.
	 * 
	 * @param flag
	 */
	default void setAutoPurgeFlag(Boolean flag) {
		setRestProperty(AUTO_PURGE_FLAG_KEY, flag);
	}

	/**
	 * Set the segmentField name.
	 * 
	 * @param fieldName
	 */
	default void setSegmentField(String fieldName) {
		setRestProperty(SEGMENT_FIELD_KEY, fieldName);
	}

	/**
	 * Return the segmentField name.
	 * 
	 * @return
	 */
	default String getSegmentField() {
		return getRestProperty(SEGMENT_FIELD_KEY);
	}

	/**
	 * Return the list of url fields.
	 * 
	 * @return
	 */
	default List<String> getURLFields() {
		Object[] value = getRestProperty(URLFIELDS_KEY);
		if (value == null) {
			return null;
		}
		String[] stringArray = Arrays.copyOf(value, value.length, String[].class);
		return Arrays.asList(stringArray);
	}

	/**
	 * Set the url fields
	 * 
	 * @param keys
	 */
	default void setURLFields(String... keys) {
		setRestProperty(URLFIELDS_KEY, keys);
	}

	@Override
	default void updateFromRest(SchemaChangeModel restChange) {
		/***
		 * Many graph databases can't handle null values. Tinkerpop blueprint contains constrains which avoid setting null values. We store empty string for the
		 * segment field name instead. It is possible to set setStandardElementConstraints for each tx to false in order to avoid such checks.
		 */
		if (restChange.getProperties().containsKey(SEGMENT_FIELD_KEY) && restChange.getProperty(SEGMENT_FIELD_KEY) == null) {
			restChange.setProperty(SEGMENT_FIELD_KEY, "");
		}
		HibFieldSchemaContainerUpdateChange.super.updateFromRest(restChange);
	}

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@SuppressWarnings("unchecked")
	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {
		if (!(container instanceof SchemaModel)) {
			throw error(BAD_REQUEST, "The provided container was no " + SchemaModel.class.getName() + " got {" + container.getClass().getName() + "}");
		}

		SchemaModel schema = (SchemaModel) HibFieldSchemaContainerUpdateChange.super.apply(container);

		// .displayField
		String displayFieldname = getDisplayField();
		if (displayFieldname != null) {
			schema.setDisplayField(displayFieldname);
		}

		String segmentFieldname = getSegmentField();
		if (segmentFieldname != null) {
			schema.setSegmentField(segmentFieldname);
		}

		// .urlFields
		List<String> urlFields = getURLFields();
		if (urlFields != null) {
			schema.setUrlFields(urlFields);
		}

		// .segmentField
		// We handle empty string as null
		if (segmentFieldname != null && isEmpty(segmentFieldname)) {
			schema.setSegmentField(null);
		}

		// .container
		Boolean containerFlag = getContainerFlag();
		if (containerFlag != null) {
			schema.setContainer(containerFlag);
		}

		// .searchIndex
		JsonObject options = getIndexOptions();
		if (options != null) {
			schema.setElasticsearch(options);
		}

		// .autoPurge
		Boolean autoPurge = getAutoPurgeFlag();
		schema.setAutoPurge(autoPurge);

		return (R) schema;
	}

	@Override
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		return Collections.emptyMap();
	}
}

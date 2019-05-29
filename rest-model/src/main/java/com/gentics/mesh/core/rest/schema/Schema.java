package com.gentics.mesh.core.rest.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Schema which is used for nodes. Various fields can be added to the schema in order build data structures for nodes.
 */
public interface Schema extends FieldSchemaContainer {

	/**
	 * Return the display field of the schema which nodes will inherit in order. This is useful when you want to unify the name that should be displayed for
	 * nodes of different types. Nodes that use the folder schema may use the display field name to display the name and blogpost nodes the field title.
	 * 
	 * @return Display field of the schema
	 */
	String getDisplayField();

	/**
	 * Set the display field value.
	 * 
	 * @param displayField
	 *            Display field
	 */
	Schema setDisplayField(String displayField);

	/**
	 * Return the container flag.
	 * 
	 * @return
	 */
	Boolean getContainer();

	/**
	 * Set the container flag for this schema. Nodes that are created using a schema which has an enabled container flag can be used as a parent for new nodes.
	 * 
	 * @param flag
	 *            Container flag value
	 */
	Schema setContainer(Boolean flag);

	/**
	 * Return the segment field name.
	 * 
	 * @return
	 */
	String getSegmentField();

	/**
	 * Set the segment field name.
	 * 
	 * @param segmentField
	 */
	Schema setSegmentField(String segmentField);

	default void validate() {
		FieldSchemaContainer.super.validate();
		// TODO make sure that the display name field only maps to string fields since NodeImpl can currently only deal with string field values for
		// displayNames
		if (!StringUtils.isEmpty(getDisplayField())) {
			if (!getFields().stream().map(FieldSchema::getName).anyMatch(e -> e.equals(getDisplayField()))) {
				throw error(BAD_REQUEST, "schema_error_displayfield_invalid", getDisplayField());
			}

			// TODO maybe we should also allow other field types
			if (!getField(getDisplayField()).isDisplayField()) {
				throw error(BAD_REQUEST, "schema_error_displayfield_type_invalid", getDisplayField());
			}
		}

		if (getUrlFields() != null) {
			for (String urlFieldName : getUrlFields()) {
				FieldSchema segmentFieldSchema = getField(urlFieldName);
				if (segmentFieldSchema == null) {
					throw error(BAD_REQUEST, "schema_error_urlfield_null", urlFieldName);
				}
				if (segmentFieldSchema != null && (!((segmentFieldSchema instanceof StringFieldSchema)
					|| (segmentFieldSchema instanceof ListFieldSchema)))) {
					throw error(BAD_REQUEST, "schema_error_urlfield_type_invalid", urlFieldName, segmentFieldSchema.getType());
				}
				if (segmentFieldSchema instanceof ListFieldSchema) {
					ListFieldSchema list = (ListFieldSchema) segmentFieldSchema;
					if (!list.getListType().equals("string")) {
						throw error(BAD_REQUEST, "schema_error_urlfield_type_invalid", urlFieldName, list.getListType());
					}
				}
			}
		}

		FieldSchema segmentFieldSchema = getField(getSegmentField());
		if (segmentFieldSchema != null && (!((segmentFieldSchema instanceof StringFieldSchema)
			|| (segmentFieldSchema instanceof BinaryFieldSchema)))) {
			throw error(BAD_REQUEST, "schema_error_segmentfield_type_invalid", segmentFieldSchema.getType());
		}

		if (getSegmentField() != null && !getFields().stream().map(FieldSchema::getName).anyMatch(e -> e.equals(getSegmentField()))) {
			throw error(BAD_REQUEST, "schema_error_segmentfield_invalid", getSegmentField());
		}
	}

	/**
	 * Return the list of url fields.
	 * 
	 * @return
	 */
	List<String> getUrlFields();

	/**
	 * Set the list of url field names.
	 * 
	 * @param urlFields
	 * @return Fluent API
	 */
	Schema setUrlFields(List<String> urlFields);

	/**
	 * Set the url field names.
	 * 
	 * @param fieldNames
	 * @return Fluent API
	 */
	default Schema setUrlFields(String... fieldNames) {
		this.setUrlFields(Arrays.asList(fieldNames));
		return this;
	}

	/**
	 * Return the auto purge flag for the schema.
	 * 
	 * @return
	 */
	Boolean getAutoPurge();

	/**
	 * Set the auto purge flag.
	 * 
	 * @param autoPurge
	 * @return
	 */
	Schema setAutoPurge(Boolean autoPurge);
}

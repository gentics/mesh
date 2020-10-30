package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.json.JsonObject;

/**
 * Change entry that contains information on how to change schema specific attributes.
 */
public interface UpdateSchemaChange extends FieldSchemaContainerUpdateChange<Schema> {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATESCHEMA;

	/**
	 * Set the displayField name.
	 *
	 * @param fieldName
	 */
	void setDisplayField(String fieldName);

	/**
	 * Return the displayField name.
	 *
	 * @return
	 */
	String getDisplayField();

	/**
	 * Set the container flag.
	 *
	 * @param flag
	 */
	void setContainerFlag(Boolean flag);

	/**
	 * Return the container flag.
	 *
	 * @return
	 */
	Boolean getContainerFlag();

	/**
	 * Return the auto purge flag.
	 *
	 * @return
	 */
	Boolean getAutoPurgeFlag();

	/**
	 * Set the auto purge flag.
	 *
	 * @param flag
	 */
	void setAutoPurgeFlag(Boolean flag);

	/**
	 * Return the versioning flag.
	 *
	 * @return The versioning flag
	 */
	Boolean getVersioningFlag();

	/**
	 * Set the versioning flag.
	 *
	 * @param flag The value for the versioning flag
	 */
	void setVersioningFlag(Boolean flag);

	/**
	 * Set the segmentField name.
	 *
	 * @param fieldName
	 */
	void setSegmentField(String fieldName);

	/**
	 * Return the segmentField name.
	 *
	 * @return
	 */
	String getSegmentField();

	/**
	 * Set the url fields
	 *
	 * @param keys
	 */
	void setURLFields(String... keys);

	/**
	 * Return the list of url fields.
	 *
	 * @return
	 */
	List<String> getURLFields();

	/**
	 * Return the index options which were stored with this change.
	 *
	 * @return
	 */
	JsonObject getIndexOptions();

	/**
	 * Set the index options.
	 *
	 * @param indexOptions
	 */
	void setIndexOptions(JsonObject indexOptions);

}

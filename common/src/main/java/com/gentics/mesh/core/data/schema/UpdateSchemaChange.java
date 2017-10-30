package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry that contains information on how to change schema specific attributes.
 */
public interface UpdateSchemaChange extends FieldSchemaContainerUpdateChange<Schema> {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATESCHEMA;

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

}

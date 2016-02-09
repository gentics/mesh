package com.gentics.mesh.core.data.schema;

import java.util.List;

/**
 * Change entry that contains information on how to change schema specific attributes.
 */
public interface UpdateSchemaChange extends SchemaChange {

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
	 * Set the field order.
	 * 
	 * @param list
	 * 
	 */
	void setOrder(String... fieldNames);

	/**
	 * Return the field order.
	 * 
	 * @return
	 */
	List<String> getOrder();

}

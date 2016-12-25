package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

/**
 * Common interface for multiple field container update changes (e.g.: {@link UpdateSchemaChange}, {@link UpdateMicroschemaChange})
 * 
 * @param <T>
 *            Type of field container
 */
public interface FieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends SchemaChange<T> {

	/**
	 * Set the field order.
	 * 
	 * @param fieldNames
	 * 
	 */
	void setOrder(String... fieldNames);

	/**
	 * Return the field order.
	 * 
	 * @return
	 */
	List<String> getOrder();

	/**
	 * Return the field container description.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the field container description.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	/**
	 * Return the field container name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the field container name.
	 * 
	 * @param name
	 */
	void setName(String name);
}

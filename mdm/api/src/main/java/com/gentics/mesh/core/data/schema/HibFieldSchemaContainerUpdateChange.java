package com.gentics.mesh.core.data.schema;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.json.JsonObject;

/**
 * Common interface for multiple field container update changes (e.g.: {@link UpdateSchemaChange}, {@link UpdateMicroschemaChange})
 * 
 * @param <T>
 *            Type of field container
 */
public interface HibFieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends HibSchemaChange<T> {

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

	/**
	 * Return the 'exclude from indexing' flag.
	 * 
	 * @return
	 */
	Boolean getNoIndex();

	/**
	 * Set the 'exclude from indexing' flag.
	 * 
	 * @param name
	 */
	void setNoIndex(Boolean noIndex);

	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {

		// .name
		String name = getName();
		if (name != null) {
			container.setName(name);
		}

		// .description
		String description = getDescription();
		if (description != null) {
			container.setDescription(description);
		}

		// .elasticsearch
		JsonObject options = getIndexOptions();
		if (options != null) {
			container.setElasticsearch(options);
		}

		// .noIndex
		Boolean noIndex = getNoIndex();
		if (noIndex != null) {
			container.setNoIndex(noIndex);
		}

		// Update the fields if the order changes
		List<String> order = getOrder();
		if (order != null) {
			List<FieldSchema> orderedList = new ArrayList<>();
			for (String fieldName : order) {
				orderedList.add(container.getField(fieldName));
			}
			container.setFields(orderedList);
		}

		return container;
	}
}


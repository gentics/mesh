package com.gentics.mesh.core.data.schema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.schema.FieldSchemaContainerUpdateChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.json.JsonObject;

/**
 * Abstract class for update changes for schemas or microschemas.
 * 
 * @param <T>
 */
public abstract class AbstractFieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends AbstractSchemaChange<T> implements
		FieldSchemaContainerUpdateChange<T> {

	@Override
	public String getName() {
		return getRestProperty(SchemaChangeModel.NAME_KEY);
	}

	@Override
	public void setName(String name) {
		setRestProperty(SchemaChangeModel.NAME_KEY, name);
	}

	@Override
	public Boolean getNoIndex() {
		return getRestProperty(SchemaChangeModel.NO_INDEX_KEY);
	}

	@Override
	public void setNoIndex(Boolean noIndex) {
		setRestProperty(SchemaChangeModel.NO_INDEX_KEY, noIndex);
	}

	@Override
	public String getDescription() {
		return getRestProperty(SchemaChangeModel.DESCRIPTION_KEY);
	}

	@Override
	public void setDescription(String description) {
		setRestProperty(SchemaChangeModel.DESCRIPTION_KEY, description);
	}

	@Override
	public List<String> getOrder() {
		Object[] fieldNames = getRestProperty(SchemaChangeModel.FIELD_ORDER_KEY);
		if (fieldNames == null) {
			return null;
		}
		String[] stringArray = Arrays.copyOf(fieldNames, fieldNames.length, String[].class);
		return Arrays.asList(stringArray);
	}

	@Override
	public void setOrder(String... fieldNames) {
		setRestProperty(SchemaChangeModel.FIELD_ORDER_KEY, fieldNames);
	}

	@Override
	public <R extends FieldSchemaContainer> R apply(R container) {

		// .name
		String name = getName();
		if (name != null) {
			container.setName(name);
		}

		// no index
		Boolean noIndex = getNoIndex();
		if (noIndex != null) {
			container.setNoIndex(noIndex);
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

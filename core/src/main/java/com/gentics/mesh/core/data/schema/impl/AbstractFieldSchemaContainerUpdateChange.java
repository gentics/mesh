package com.gentics.mesh.core.data.schema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.schema.FieldSchemaContainerUpdateChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public abstract class AbstractFieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends AbstractSchemaChange<T>
		implements FieldSchemaContainerUpdateChange<T> {

	private static final String DESCRIPTION_KEY = "description";
	private static final String FIELD_ORDER_KEY = "order";

	@Override
	public String getDescription() {
		return getProperty(DESCRIPTION_KEY);
	}

	@Override
	public void setDescription(String description) {
		setProperty(DESCRIPTION_KEY, description);
	}

	@Override
	public List<String> getOrder() {
		String[] fieldNames = getProperty(FIELD_ORDER_KEY);
		if (fieldNames == null) {
			return null;
		}
		return Arrays.asList(fieldNames);
	}

	@Override
	public void setOrder(String... fieldNames) {
		setProperty(FIELD_ORDER_KEY, fieldNames);
	}

	@Override
	public T apply(T container) {

		String description = getDescription();
		if (description != null) {
			container.setDescription(description);
		}

		List<String> order = getOrder();
		if (order != null) {
			List<FieldSchema> orderedList = new ArrayList<>();
			for (String name : order) {
				orderedList.add(container.getField(name));
			}
			container.setFields(orderedList);
		}

		return null;
	}

}

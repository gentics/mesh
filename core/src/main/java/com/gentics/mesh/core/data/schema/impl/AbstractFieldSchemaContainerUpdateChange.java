package com.gentics.mesh.core.data.schema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.schema.FieldSchemaContainerUpdateChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public abstract class AbstractFieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends AbstractSchemaChange<T>
		implements FieldSchemaContainerUpdateChange<T> {

	@Override
	public String getName() {
		return getProperty(SchemaChangeModel.NAME_KEY);
	}

	@Override
	public void setName(String name) {
		setProperty(SchemaChangeModel.NAME_KEY, name);
	}

	@Override
	public String getDescription() {
		return getProperty(SchemaChangeModel.DESCRIPTION_KEY);
	}

	@Override
	public void setDescription(String description) {
		setProperty(SchemaChangeModel.DESCRIPTION_KEY, description);
	}

	@Override
	public List<String> getOrder() {
		String[] fieldNames = getProperty(SchemaChangeModel.FIELD_ORDER_KEY);
		if (fieldNames == null) {
			return null;
		}
		return Arrays.asList(fieldNames);
	}

	@Override
	public void setOrder(String... fieldNames) {
		setProperty(SchemaChangeModel.FIELD_ORDER_KEY, fieldNames);
	}

	@Override
	public T apply(T container) {

		String name = getName();
		if (name != null) {
			container.setName(name);
		}

		String description = getDescription();
		if (description != null) {
			container.setDescription(description);
		}

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

package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;

public class MicronodeResponseAssert extends AbstractAssert<MicronodeResponseAssert, MicronodeResponse> {

	public MicronodeResponseAssert(MicronodeResponse actual) {
		super(actual, MicronodeResponseAssert.class);
	}

	public MicronodeResponseAssert matches(MicronodeResponse expected, MicroschemaModel schema) {
		if (expected == null) {
			assertNull(descriptionText() + " must be null", actual);
			return this;
		}
		for (FieldSchema fieldSchema : schema.getFields()) {
			String key = fieldSchema.getName();

			switch (fieldSchema.getType()) {
			case "html":
				assertThat(expected.getFields().getHtmlField(key)).isNotNull();
				assertThat(actual.getFields().getHtmlField(key).getHTML()).as("Field " + key)
						.isEqualTo(expected.getFields().getHtmlField(key).getHTML());
				break;
			case "binary":
				break;
			case "boolean":
				assertThat(expected.getFields().getBooleanField(key)).isNotNull();
				assertThat(actual.getFields().getBooleanField(key).getValue()).as("Field " + key)
						.isEqualTo(expected.getFields().getBooleanField(key).getValue());
				break;
			case "date":
				assertThat(expected.getFields().getDateField(key)).isNotNull();
				assertThat(actual.getFields().getDateField(key).getDate()).as("Field " + key)
						.isEqualTo(expected.getFields().getDateField(key).getDate());
				break;
			case "node":
				assertThat(expected.getFields().getNodeField(key)).isNotNull();
				assertThat(actual.getFields().getNodeField(key).getUuid()).as("Field " + key)
						.isEqualTo(expected.getFields().getNodeField(key).getUuid());
				break;
			case "string":
				assertThat(expected.getFields().getStringField(key)).isNotNull();
				assertThat(actual.getFields().getStringField(key).getString()).as("Field " + key)
						.isEqualTo(expected.getFields().getStringField(key).getString());
				break;
			case "number":
				assertThat(expected.getFields().getNumberField(key)).isNotNull();
				assertThat(actual.getFields().getNumberField(key).getNumber()).as("Field " + key)
						.isEqualTo(expected.getFields().getNumberField(key).getNumber());
				break;
			case "list":

				Field field = actual.getFields().getField(key, fieldSchema);
				if (field instanceof NodeFieldList) {
					// compare list of nodes by comparing their uuids
					assertThat(((NodeFieldList) field).getItems()).usingElementComparator((a, b) -> {
						return a.getUuid().compareTo(b.getUuid());
					}).containsExactlyElementsOf(expected.getFields().getNodeFieldList(key).getItems());
				} else if (field instanceof FieldList) {
					//TODO handle lists
//					assertThat(((FieldList<?>) field).getItems())
//							.containsExactlyElementsOf(expected.getFields().get(key, FieldList.class).getItems());
				}

			}

		}

		return this;
	}

	/**
	 * Assert that the MicronodeResponse contains the given string field
	 * @param name field name
	 * @param value field value
	 * @return fluent API
	 */
	public MicronodeResponseAssert hasStringField(String name, String value) {
		assertThat(actual.getFields().getStringField(name)).as("Field " + name).isNotNull();
		assertThat(actual.getFields().getStringField(name).getString()).as("Field " + name + " value").isEqualTo(value);
		return this;
	}
}

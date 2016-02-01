package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

public class MicronodeResponseAssert extends AbstractAssert<MicronodeResponseAssert, MicronodeResponse> {

	public MicronodeResponseAssert(MicronodeResponse actual) {
		super(actual, MicronodeResponseAssert.class);
	}

	@SuppressWarnings("unchecked")
	public MicronodeResponseAssert matches(MicronodeResponse expected) {
		if (expected == null) {
			assertNull(descriptionText() + " must be null", actual);
			return this;
		}

		for (String key : actual.getFields().keySet()) {
			Field field = actual.getField(key);
			if (field instanceof BinaryField) {
				// TODO implement assertions
			}
			if (field instanceof BooleanField) {
				assertThat(expected.getField(key, BooleanField.class)).isNotNull();
				assertThat(actual.getField(key, BooleanField.class).getValue()).as("Field " + key)
						.isEqualTo(expected.getField(key, BooleanField.class).getValue());
			}
			if (field instanceof DateField) {
				assertThat(expected.getField(key, DateField.class)).isNotNull();
				assertThat(actual.getField(key, DateField.class).getDate()).as("Field " + key)
						.isEqualTo(expected.getField(key, DateField.class).getDate());
			}
			if (field instanceof HtmlField) {
				assertThat(expected.getField(key, HtmlField.class)).isNotNull();
				assertThat(actual.getField(key, HtmlField.class).getHTML()).as("Field " + key)
						.isEqualTo(expected.getField(key, HtmlField.class).getHTML());
			}
			if (field instanceof NodeField) {
				assertThat(expected.getField(key, NodeField.class)).isNotNull();
				assertThat(actual.getField(key, NodeField.class).getUuid()).as("Field " + key)
						.isEqualTo(expected.getField(key, NodeField.class).getUuid());
			}
			if (field instanceof NumberField) {
				assertThat(expected.getField(key, NumberField.class)).isNotNull();
				assertThat(actual.getField(key, NumberField.class).getNumber()).as("Field " + key)
						.isEqualTo(expected.getField(key, NumberField.class).getNumber());
			}
			if (field instanceof StringField) {
				assertThat(expected.getField(key, StringField.class)).isNotNull();
				assertThat(actual.getField(key, StringField.class).getString()).as("Field " + key)
						.isEqualTo(expected.getField(key, StringField.class).getString());
			}

			if (field instanceof NodeFieldList) {
				// compare list of nodes by comparing their uuids
				assertThat(((NodeFieldList) field).getItems()).usingElementComparator((a, b) -> {
					return a.getUuid().compareTo(b.getUuid());
				}).containsExactlyElementsOf(expected.getField(key, NodeFieldList.class).getItems());
			} else if (field instanceof FieldList) {
				assertThat(((FieldList<?>) field).getItems())
						.containsExactlyElementsOf(expected.getField(key, FieldList.class).getItems());
			}
		}

		return this;
	}
}

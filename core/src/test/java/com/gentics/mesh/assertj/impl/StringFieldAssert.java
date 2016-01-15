package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.field.StringField;

public class StringFieldAssert extends AbstractAssert<StringFieldAssert, StringField> {

	public StringFieldAssert(StringField actual) {
		super(actual, StringFieldAssert.class);
	}

	public StringFieldAssert matches(StringField expected) {
		if (expected == null) {
			assertNull(descriptionText() + " must be null", actual);
			return this;
		}
		assertNotNull(descriptionText() + " must not be null", actual);
		assertEquals(descriptionText() + " value", expected.getString(), actual.getString());
		return this;
	}
}

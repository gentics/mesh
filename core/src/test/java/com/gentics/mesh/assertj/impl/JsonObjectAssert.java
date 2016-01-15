package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.AbstractAssert;

import io.vertx.core.json.JsonObject;

public class JsonObjectAssert extends AbstractAssert<JsonObjectAssert, JsonObject> {
	/**
	 * Key to check
	 */
	protected String key;

	public JsonObjectAssert(JsonObject actual) {
		super(actual, JsonObjectAssert.class);
	}

	public JsonObjectAssert key(String key) {
		this.key = key;
		return this;
	}

	public JsonObjectAssert matches(Object expected) {
		assertNotNull(descriptionText() + " cannot be matched without specifying key first", key);
		assertNotNull(descriptionText() + " JsonObject must not be null", actual);
		assertEquals(descriptionText() + " key " + key, expected, actual.getValue(key));
		return this;
	}
}

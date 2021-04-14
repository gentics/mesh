package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Function;

import org.assertj.core.api.AbstractAssert;
import org.codehaus.jettison.json.JSONException;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonArrayAssert extends AbstractAssert<JsonArrayAssert, JsonArray> {
	protected String key;

	public JsonArrayAssert(JsonArray actual) {
		super(actual, JsonArrayAssert.class);
	}

	public JsonArrayAssert key(String key) {
		this.key = key;
		return this;
	}

	public <T> JsonArrayAssert matches(@SuppressWarnings("unchecked") T...expected) throws JSONException {
		if (expected == null) {
			assertNull(descriptionText() + " JSON Array is expected to be null", actual);
			return this;
		}
		assertNotNull(descriptionText() + " JSON Array is expected to not be null", actual);
		assertEquals(descriptionText() + " # of entries", expected.length, actual.size());

		for (int i = 0; i < expected.length; i++) {
			if (key != null) {
				JsonObject entryObject = actual.getJsonObject(i);
				assertNotNull(descriptionText() + " entry #" + i +" must be a JSON object", entryObject);
				assertEquals(descriptionText() + " entry #" + i, expected[i], entryObject.getValue(key));
			} else {
				assertEquals(descriptionText() + " entry #" + i, expected[i], actual.getValue(i));
			}
		}

		return this;
	}

	public JsonArrayAssert containsJsonObjectHashesInAnyOrder(Function<JsonObject, Integer> hashMapper, Integer... expectedHashes) {
		Multiset<Integer> actualHashes = ImmutableMultiset.copyOf(actual.stream()
			.map(item -> hashMapper.apply((JsonObject) item))
			.iterator());

		assertThat(actualHashes).containsOnly(expectedHashes);

		return this;
	}
}

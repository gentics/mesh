package com.gentics.mesh.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;

public class MeshJsonNodeTest {

	@Test
	public void testNull() {
		assertTrue(parse("{\"node\": null}").getNode().isNull());
	}

	@Test
	public void testNotSet() {
		assertNull(parse("{}").getNode());
	}

	@Test
	public void testSet() {
		assertTrue(parse("{\"node\": {}}").getNode().isObject());
	}

	private Pojo parse(String json) {
		return JsonUtil.readValue(json, Pojo.class);
	}

	public static class Pojo {
		private JsonNode node;

		public JsonNode getNode() {
			return node;
		}

		public Pojo setNode(JsonNode node) {
			this.node = node;
			return this;
		}
	}
}

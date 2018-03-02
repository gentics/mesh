package com.gentics.mesh.core.rest.schema.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SchemaModelTest {

	@Test
	public void testIndexConfig() {
		SchemaModel model = new SchemaModelImpl();
		model.setElasticsearch(new JsonObject().put("key", "value").put("array", new JsonArray().add("A").add("B")));
		StringFieldSchema stringField = new StringFieldSchemaImpl();
		stringField.setName("someName");
		stringField.setRequired(true);
		stringField.setLabel("someLabel");
		stringField.setElasticsearch(new JsonObject().put("key", "value"));
		model.addField(stringField);
		String json = model.toJson();
		System.out.println(json);

		SchemaModel readModel = JsonUtil.readValue(json, SchemaModelImpl.class);
		assertEquals("value", readModel.getElasticsearch().getString("key"));

	}
}

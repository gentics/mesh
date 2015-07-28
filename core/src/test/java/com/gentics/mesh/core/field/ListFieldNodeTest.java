package com.gentics.mesh.core.field;

import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class ListFieldNodeTest extends AbstractDBTest {

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

	@Test
	public void testNodeListTransformation() throws IOException {
		Node node = data().getFolder("2015");
		Node newsNode = data().getFolder("news");

		Schema schema = node.getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("dummyList");
		listFieldSchema.setListType("node");
		schema.addField(listFieldSchema);

		node.getSchemaContainer().setSchema(schema);

		NodeFieldContainer container = node.getFieldContainer(data().getEnglish());

		NodeFieldList list = container.createNodeList("dummyList");
		list.createNode("1", newsNode);
		list.createNode("2", newsNode);

		RoutingContext rc = getMockedRoutingContext("lang=en");
		node.transformToRest(rc, rh -> {
			NodeResponse response = rh.result();
			String json = JsonUtil.toJson(response);
			System.out.println(json);
			assertNotNull(response);
		});

	}
}

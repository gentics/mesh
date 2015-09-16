package com.gentics.mesh.core.field.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.AbstractFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class GaphListFieldNodeTest extends AbstractBasicDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testNodeListTransformation() throws Exception {
		Node node = folder("2015");
		Node newsNode = folder("news");

		Schema schema = node.getSchema();
		ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
		nodeListFieldSchema.setName("nodeList");
		nodeListFieldSchema.setListType("node");
		schema.addField(nodeListFieldSchema);

		ListFieldSchema stringListFieldSchema = new ListFieldSchemaImpl();
		stringListFieldSchema.setName("stringList");
		stringListFieldSchema.setListType("string");
		schema.addField(stringListFieldSchema);

		ListFieldSchema htmlListFieldSchema = new ListFieldSchemaImpl();
		htmlListFieldSchema.setName("htmlList");
		htmlListFieldSchema.setListType("html");
		schema.addField(htmlListFieldSchema);

		ListFieldSchema numberListFieldSchema = new ListFieldSchemaImpl();
		numberListFieldSchema.setName("numberList");
		numberListFieldSchema.setListType("number");
		schema.addField(numberListFieldSchema);

		ListFieldSchema booleanListFieldSchema = new ListFieldSchemaImpl();
		booleanListFieldSchema.setName("booleanList");
		booleanListFieldSchema.setListType("boolean");
		schema.addField(booleanListFieldSchema);

		ListFieldSchema dateListFieldSchema = new ListFieldSchemaImpl();
		dateListFieldSchema.setName("dateList");
		dateListFieldSchema.setListType("date");
		schema.addField(dateListFieldSchema);

		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());

		NodeGraphFieldList nodeList = container.createNodeList("nodeList");
		nodeList.createNode("1", newsNode);
		nodeList.createNode("2", newsNode);

		BooleanGraphFieldList booleanList = container.createBooleanList("booleanList");
		booleanList.createBoolean(true);
		booleanList.createBoolean(null);
		booleanList.createBoolean(false);

		NumberGraphFieldList numberList = container.createNumberList("numberList");
		numberList.createNumber("1");
		numberList.createNumber("1.11");

		DateGraphFieldList dateList = container.createDateList("dateList");
		dateList.createDate("01.01.1971");
		dateList.createDate("01.01.1972");

		StringGraphFieldList stringList = container.createStringList("stringList");
		stringList.createString("dummyString1");
		stringList.createString("dummyString2");

		HtmlGraphFieldList htmlList = container.createHTMLList("htmlList");
		htmlList.createHTML("some<b>html</b>");
		htmlList.createHTML("some<b>more html</b>");

		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);

		assertList(2, "stringList", StringFieldListImpl.class, response);
		assertList(2, "htmlList", StringFieldListImpl.class, response);
		assertList(2, "dateList", DateFieldListImpl.class, response);
		assertList(2, "numberList", NumberFieldListImpl.class, response);
		assertList(2, "nodeList", NodeFieldListImpl.class, response);
		assertList(3, "booleanList", BooleanFieldListImpl.class, response);
		// assertList(0, "microschemaList", MicroschemaFieldListImpl.class, response);

	}

	private <T extends AbstractFieldList<?>> void assertList(int expectedItems, String fieldKey, Class<T> classOfT, NodeResponse response) {
		T deserializedList = response.getField(fieldKey, classOfT);
		assertNotNull(deserializedList);
		assertEquals(expectedItems, deserializedList.getItems().size());
	}
}

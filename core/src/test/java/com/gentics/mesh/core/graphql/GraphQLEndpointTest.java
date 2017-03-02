package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointTest extends AbstractMeshTest {

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

	@Test
	public void testNodeQuery() throws JSONException {
		String contentUuid = db().noTx(() -> content().getUuid());
		String creationDate = db().noTx(() -> content().getCreationDate());
		try (NoTx noTx = db().noTx()) {
			Node node = folder("2015");
			Node node2 = content();
			Node node3 = folder("2014");

			// Update schema
			Schema schema = schemaContainer("folder").getLatestVersion()
					.getSchema();
			NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
			nodeFieldSchema.setName("nodeRef");
			nodeFieldSchema.setLabel("Some label");
			nodeFieldSchema.setAllowedSchemas("folder");
			schema.addField(nodeFieldSchema);

			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName("binary");
			schema.addField(binaryFieldSchema);

			NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
			numberFieldSchema.setName("number");
			schema.addField(numberFieldSchema);

			DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
			dateFieldSchema.setName("date");
			schema.addField(dateFieldSchema);

			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName("html");
			schema.addField(htmlFieldSchema);

			BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
			booleanFieldSchema.setName("boolean");
			schema.addField(booleanFieldSchema);

			ListFieldSchema stringListSchema = new ListFieldSchemaImpl();
			stringListSchema.setListType("string");
			stringListSchema.setName("stringList");
			schema.addField(stringListSchema);

			ListFieldSchema dateListSchema = new ListFieldSchemaImpl();
			dateListSchema.setListType("date");
			dateListSchema.setName("dateList");
			schema.addField(dateListSchema);

			ListFieldSchema nodeListSchema = new ListFieldSchemaImpl();
			nodeListSchema.setListType("node");
			nodeListSchema.setName("nodeList");
			schema.addField(nodeListSchema);

			ListFieldSchema htmlListSchema = new ListFieldSchemaImpl();
			htmlListSchema.setListType("html");
			htmlListSchema.setName("htmlList");
			schema.addField(htmlListSchema);

			ListFieldSchema booleanListSchema = new ListFieldSchemaImpl();
			booleanListSchema.setListType("boolean");
			booleanListSchema.setName("booleanList");
			schema.addField(booleanListSchema);

			ListFieldSchema numberListSchema = new ListFieldSchemaImpl();
			numberListSchema.setListType("number");
			numberListSchema.setName("numberList");
			schema.addField(numberListSchema);

			schemaContainer("folder").getLatestVersion()
					.setSchema(schema);

			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			// node
			container.createNode("nodeRef", node2);

			//number
			container.createNumber("number")
					.setNumber(42.1);

			//date
			container.createDate("date")
					.setDate(System.currentTimeMillis());

			//html
			container.createHTML("html")
					.setHtml("some html");

			//boolean
			container.createBoolean("boolean")
					.setBoolean(true);

			// binary
			container.createBinary("binary")
					.setSHA512Sum("hashsumvalue")
					.setImageHeight(10)
					.setImageWidth(20)
					.setImageDominantColor("00FF00")
					.setMimeType("image/jpeg")
					.setFileSize(2048);

			// stringList
			StringGraphFieldList stringList = container.createStringList("stringList");
			stringList.createString("A");
			stringList.createString("B");
			stringList.createString("C");

			// htmlList
			HtmlGraphFieldList htmlList = container.createHTMLList("htmlList");
			htmlList.createHTML("A");
			htmlList.createHTML("B");
			htmlList.createHTML("C");

			// dateList
			DateGraphFieldList dateList = container.createDateList("dateList");
			dateList.createDate(System.currentTimeMillis());
			dateList.createDate(System.currentTimeMillis());
			dateList.createDate(System.currentTimeMillis());

			// numberList
			NumberGraphFieldList numberList = container.createNumberList("numberList");
			numberList.createNumber(System.currentTimeMillis());
			numberList.createNumber(System.currentTimeMillis());
			numberList.createNumber(System.currentTimeMillis());

			// booleanList
			BooleanGraphFieldList booleanList = container.createBooleanList("booleanList");
			booleanList.createBoolean(true);
			booleanList.createBoolean(null);
			booleanList.createBoolean(false);

			// nodeList
			NodeGraphFieldList nodeList = container.createNodeList("nodeList");
			nodeList.createNode("0", node2);
			nodeList.createNode("1", node3);

		}
		//JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{ tagFamilies(name: \"colors\") { name, creator {firstname, lastname}, tags(page: 1, perPage:1) {name}}, schemas(name:\"content\") {name}, nodes(uuid:\"" + contentUuid + "\"){uuid, languagePaths(linkType: FULL) {languageTag, link}, availableLanguages, project {name, rootNode {uuid}}, created, creator { username, groups { name, roles {name} } }}}"));

		JsonObject response = call(() -> client().graphql(PROJECT_NAME, getQuery("node2-query")));

		System.out.println(response.encodePrettily());
		//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "', 'created': '" + creationDate + "'}}}", response);

		//		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\") {uuid, fields { ... on content { name, content }}}}"));
		//		System.out.println(response.toString());
		//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);
	}

	private String getQuery(String name) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream("/graphql/" + name));
	}

}

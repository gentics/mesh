package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLEndpointTest extends AbstractMeshTest {

	@Parameters(name = "query={0}")
	public static List<String> paramData() {
		List<String> testQueries = new ArrayList<>();
		testQueries.add("full-query");
		testQueries.add("role-user-group-query");
		testQueries.add("group-query");
		testQueries.add("node-relations-query");
		testQueries.add("schema-query");
		testQueries.add("microschema-query");
		testQueries.add("paging-query");
		testQueries.add("tagFamily-query");
		testQueries.add("node2-query");
		testQueries.add("project-query");
		testQueries.add("tag-query");
		testQueries.add("node-fields-query");
		testQueries.add("release-query");
		testQueries.add("user-query");
		return testQueries;
	}

	private final String queryName;

	public GraphQLEndpointTest(String queryName) {
		this.queryName = queryName;
	}

	@Test
	public void testNodeQuery() throws JSONException, IOException {
//		String contentUuid = db().noTx(() -> content().getUuid());
//		String creationDate = db().noTx(() -> content().getCreationDate());
//		String uuid = db().noTx(() -> folder("2015").getUuid());
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

			MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
			micronodeFieldSchema.setAllowedMicroSchemas("vcard");
			micronodeFieldSchema.setName("micronode");
			schema.addField(micronodeFieldSchema);

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

			// micronode
			MicronodeGraphField micronodeField = container.createMicronode("micronode", microschemaContainer("vcard").getLatestVersion());
			micronodeField.getMicronode()
					.createString("firstName")
					.setString("Joe");
			micronodeField.getMicronode()
					.createString("lastName")
					.setString("Doe");
			micronodeField.getMicronode()
					.createString("address")
					.setString("Somewhere");
			micronodeField.getMicronode()
					.createString("postcode")
					.setString("1010");

			//folder("news").getChildren().forEach(e -> role().revokePermissions(e, GraphPermission.READ_PERM));
		}
		//JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{ tagFamilies(name: \"colors\") { name, creator {firstname, lastname}, tags(page: 1, perPage:1) {name}}, schemas(name:\"content\") {name}, nodes(uuid:\"" + contentUuid + "\"){uuid, languagePaths(linkType: FULL) {languageTag, link}, availableLanguages, project {name, rootNode {uuid}}, created, creator { username, groups { name, roles {name} } }}}"));

		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, getQuery(queryName)));
		System.out.println(response.encodePrettily());
		assertThat(response).compliesToAssertions(queryName);

		//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "', 'created': '" + creationDate + "'}}}", response);

		//		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\") {uuid, fields { ... on content { name, content }}}}"));
		//		System.out.println(response.toString());
		//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);
	}

}

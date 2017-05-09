package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
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
		testQueries.add("schema-query");
		testQueries.add("microschema-query");
		testQueries.add("paging-query");
		testQueries.add("tagFamily-query");
		testQueries.add("node-query");
		testQueries.add("node-query");
		testQueries.add("nodes-query");
		testQueries.add("node-language-fallback-query");
		testQueries.add("node-webroot-query");
		testQueries.add("node-relations-query");
		testQueries.add("node-fields-query");
		testQueries.add("node-fields-link-resolve-query");
		testQueries.add("project-query");
		testQueries.add("tag-query");
		testQueries.add("release-query");
		testQueries.add("user-query");
		testQueries.add("mesh-query");
		return testQueries;
	}

	private final String queryName;

	public GraphQLEndpointTest(String queryName) {
		this.queryName = queryName;
	}

	@Test
	public void testNodeQuery() throws JSONException, IOException, ParseException {
		String staticUuid = "43ee8f9ff71e4016ae8f9ff71e10161c";
		// String contentUuid = db().noTx(() -> content().getUuid());
		// String creationDate = db().noTx(() -> content().getCreationDate());
		// String uuid = db().noTx(() -> folder("2015").getUuid());

		// 1. Create the microschema
		MicroschemaCreateRequest microschemaRequest = new MicroschemaCreateRequest();
		microschemaRequest.setName("TestMicroschema");
		microschemaRequest.addField(FieldUtil.createStringFieldSchema("text"));
		microschemaRequest.addField(FieldUtil.createNodeFieldSchema("nodeRef").setAllowedSchemas("content"));
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(microschemaRequest));
		String microschemaUuid = microschemaResponse.getUuid();

		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschemaUuid));

		try (NoTx noTx = db().noTx()) {
			Node node = folder("2015");
			Node node2 = content();
			node2.setUuid(staticUuid);
			Node node3 = folder("2014");

			// Update schema
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
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

			HtmlFieldSchema htmlLinkFieldSchema = new HtmlFieldSchemaImpl();
			htmlLinkFieldSchema.setName("htmlLink");
			schema.addField(htmlLinkFieldSchema);

			StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName("string");
			schema.addField(stringFieldSchema);

			StringFieldSchema stringLinkFieldSchema = new StringFieldSchemaImpl();
			stringLinkFieldSchema.setName("stringLink");
			schema.addField(stringLinkFieldSchema);

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

			ListFieldSchema micronodeListSchema = new ListFieldSchemaImpl();
			micronodeListSchema.setListType("micronode");
			micronodeListSchema.setName("micronodeList");
			schema.addField(micronodeListSchema);

			MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
			micronodeFieldSchema.setAllowedMicroSchemas("vcard");
			micronodeFieldSchema.setName("micronode");
			schema.addField(micronodeFieldSchema);

			schemaContainer("folder").getLatestVersion().setSchema(schema);

			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");

			// node
			container.createNode("nodeRef", node2);

			// number
			container.createNumber("number").setNumber(42.1);

			// date
			long milisec = dateToMilis("2012-07-11 10:55:21");
			container.createDate("date").setDate(milisec);

			// html
			container.createHTML("html").setHtml("some html");

			// htmlLink
			container.createHTML("htmlLink").setHtml("Link: {{mesh.link(\"" + staticUuid + "\", \"en\")}}");

			// string
			container.createString("string").setString("some string");

			// stringLink
			container.createString("stringLink").setString("Link: {{mesh.link(\"" + staticUuid + "\", \"en\")}}");

			// boolean
			container.createBoolean("boolean").setBoolean(true);

			// binary
			container.createBinary("binary").setSHA512Sum("hashsumvalue").setImageHeight(10).setImageWidth(20).setImageDominantColor("00FF00")
					.setMimeType("image/jpeg").setFileSize(2048);

			// stringList
			StringGraphFieldList stringList = container.createStringList("stringList");
			stringList.createString("A");
			stringList.createString("B");
			stringList.createString("C");
			stringList.createString("D Link: {{mesh.link(\"" + staticUuid + "\", \"en\")}}");

			// htmlList
			HtmlGraphFieldList htmlList = container.createHTMLList("htmlList");
			htmlList.createHTML("A");
			htmlList.createHTML("B");
			htmlList.createHTML("C");
			htmlList.createHTML("D Link: {{mesh.link(\"" + staticUuid + "\", \"en\")}}");

			// dateList
			DateGraphFieldList dateList = container.createDateList("dateList");
			dateList.createDate(dateToMilis("2012-07-11 10:55:21"));
			dateList.createDate(dateToMilis("2014-07-11 10:55:30"));
			dateList.createDate(dateToMilis("2000-07-11 10:55:00"));

			// numberList
			NumberGraphFieldList numberList = container.createNumberList("numberList");
			numberList.createNumber(42L);
			numberList.createNumber(1337);
			numberList.createNumber(0.314f);

			// booleanList
			BooleanGraphFieldList booleanList = container.createBooleanList("booleanList");
			booleanList.createBoolean(true);
			booleanList.createBoolean(null);
			booleanList.createBoolean(false);

			// nodeList
			NodeGraphFieldList nodeList = container.createNodeList("nodeList");
			nodeList.createNode("0", node2);
			nodeList.createNode("1", node3);

			// micronodeList
			MicronodeGraphFieldList micronodeList = container.createMicronodeFieldList("micronodeList");
			Micronode firstMicronode = micronodeList.createMicronode();
			firstMicronode.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			firstMicronode.createString("firstName").setString("Joe");
			firstMicronode.createString("lastName").setString("Doe");
			firstMicronode.createString("address").setString("Somewhere");
			firstMicronode.createString("postcode").setString("1010");

			Micronode secondMicronode = micronodeList.createMicronode();
			secondMicronode.setSchemaContainerVersion(boot().microschemaContainerRoot().findByUuid(microschemaUuid).getLatestVersion());
			secondMicronode.createString("text").setString("Joe");
			secondMicronode.createNode("nodeRef", content());

			// micronode
			MicronodeGraphField micronodeField = container.createMicronode("micronode", microschemaContainer("vcard").getLatestVersion());
			micronodeField.getMicronode().createString("firstName").setString("Joe");
			micronodeField.getMicronode().createString("lastName").setString("Doe");
			micronodeField.getMicronode().createString("address").setString("Somewhere");
			micronodeField.getMicronode().createString("postcode").setString("1010");

			// folder("news").getChildren().forEach(e -> role().revokePermissions(e, GraphPermission.READ_PERM));
		}
		// JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{ tagFamilies(name: \"colors\") { name, creator {firstname, lastname}, tags(page: 1,
		// perPage:1) {name}}, schemas(name:\"content\") {name}, nodes(uuid:\"" + contentUuid + "\"){uuid, languagePaths(linkType: FULL) {languageTag, link},
		// availableLanguages, project {name, rootNode {uuid}}, created, creator { username, groups { name, roles {name} } }}}"));

		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		System.out.println(response.encodePrettily());
		assertThat(response).compliesToAssertions(queryName);

		// MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "', 'created': '" + creationDate + "'}}}", response);

		// JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\") {uuid, fields { ... on content { name, content
		// }}}}"));
		// System.out.println(response.toString());
		// MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);
	}

	private long dateToMilis(String date) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date).getTime();
	}

}

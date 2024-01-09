package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.CONTENT_UUID;
import static com.gentics.mesh.test.TestDataProvider.NEWS_UUID;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static java.util.Objects.hash;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.assertj.impl.JsonObjectAssert;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graphql.javafilter.JavaGraphQLEndpointTest;
import com.gentics.mesh.core.graphql.nativefilter.NativeGraphQLEndpointTest;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointTest extends AbstractMeshTest {

	protected static final String NODE_WITH_LINKS_UUID = "8d2f5769fe114353af5769fe11e35355";
	protected static final String NODE_WITH_NODE_REF_UUID = "e8f5c7875b2f49a7b5c7875b2fa9a718";
	public static final String SCHEMA_UUID = "SCHEMA_UUID";

	protected final String queryName;

	protected final boolean withMicroschema;

	private final boolean withBranchPathPrefix;

	protected final String version;
	protected final String apiVersion;

	protected final Consumer<JsonObject> assertion;
	protected MeshRestClient client;

	private static final List<String> DATES = List.of(
			"2012-07-11 08:55:21",
			"2014-07-11 10:55:30",
			"2000-07-11 10:55:00"
	);

	/**
	 * Default constructor.
	 *
	 * <p>
	 * When <code>assertion</code> is <code>null</code> the result of the GraphQL query is passed to
	 * {@link JsonObjectAssert#compliesToAssertions(String)} which will check the assertions annotated in the
	 * GraphQL query comments.
	 * </p>
	 *
	 * @param queryName The filename of the GraphQL query to use
	 * @param withMicroschema Whether to use micro schemas
	 * @param withBranchPathPrefix whether the branch should have a path prefix set
	 * @param version Whether to use the <code>draft</code> or <code>published</code> version
	 * @param assertion A custom assertion to be applied on the GraphQL query result
	 */
	public GraphQLEndpointTest(String queryName, boolean withMicroschema, boolean withBranchPathPrefix, String version, Consumer<JsonObject> assertion, String apiVersion) {
		this.queryName = queryName;
		this.withMicroschema = withMicroschema;
		this.withBranchPathPrefix = withBranchPathPrefix;
		this.version = version;
		this.assertion = assertion;
		this.apiVersion = apiVersion;
	}

	protected static Stream<List<Object>> queries() {
		return Stream.<List<Object>>of(
				Arrays.asList("full-query", true, false, "draft"),
				Arrays.asList("role-user-group-query", true, false, "draft"),
				Arrays.asList("group-query", true, false, "draft"),
				Arrays.asList("schema-query", true, false, "draft"),
				// Arrays.asList("schema-projects-query", true, false, "draft"),
				Arrays.asList("microschema-query", true, false, "draft"),
				Arrays.asList("paging-query", true, false, "draft"),
				Arrays.asList("tagFamily-query", true, false, "draft"),
				Arrays.asList("node-query", true, false, "draft"),
				Arrays.asList("node-tag-query", true, false, "draft"),
				Arrays.asList("nodes-query", true, false, "draft"),
				Arrays.asList("nodes-query-by-uuids", true, false, "draft"),
				Arrays.asList("node-breadcrumb-query", true, false, "draft"),
				Arrays.asList("node-breadcrumb-query-with-lang", true, false, "draft"),
				Arrays.asList("node-language-fallback-query", true, false, "draft"),
				Arrays.asList("node-languages-query", true, false, "draft", (Consumer<JsonObject>) GraphQLEndpointTest::checkNodeLanguageContent),
				Arrays.asList("node-not-found-webroot-query", true, false, "draft"),
				Arrays.asList("node-webroot-query", true, false, "draft"),
				Arrays.asList("node-webroot-urlfield-query", true, false, "draft"),
				Arrays.asList("node-relations-query", true, false, "draft"),
				Arrays.asList("node-fields-query", true, false, "draft"),
				Arrays.asList("node-fields-no-microschema-query", false, false, "draft"),
				Arrays.asList("node/link/webroot", true, false, "draft"),
				Arrays.asList("node/link/webroot-medium", true, false, "draft"),
				Arrays.asList("node/link/webroot-short", true, false, "draft"),
				Arrays.asList("node/link/children", true, false, "draft", (Consumer<JsonObject>) GraphQLEndpointTest::checkNodeLinkChildrenResponse),
				Arrays.asList("node/link/webroot-language", true, false, "draft"),
				Arrays.asList("node/link/reference", true, false, "draft"),
				Arrays.asList("node-field-list-path-query", true, false, "draft"),
				Arrays.asList("project-query", true, false, "draft"),
				Arrays.asList("tag-query", true, false, "draft"),
				Arrays.asList("branch-query", true, true, "draft"),
				Arrays.asList("user-query", true, false, "draft"),
				Arrays.asList("microschema-projects-query", true, false, "draft"),
				Arrays.asList("node-version-published-query", true, false, "published"),
				Arrays.asList("node/breadcrumb-root", true, false, "draft"),
				Arrays.asList("node/versionslist", true, false, "draft"),
				Arrays.asList("permissions", true, false, "draft"),
				Arrays.asList("user-node-reference", true, false, "draft"),
				Arrays.asList("filtering/children", true, false, "draft"),
				Arrays.asList("filtering/nodes-creator-editor", true, false, "draft"),
				Arrays.asList("filtering/groups", true, false, "draft"),
				Arrays.asList("filtering/roles", true, false, "draft"),
				Arrays.asList("filtering/users", true, false, "draft"),
				Arrays.asList("filtering/nodes-string-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-boolean-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-number-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-date-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-stringlist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-numberlist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-booleanlist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-datelist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-htmllist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-node-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-micronode-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-binary-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-s3binary-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-nodelist-field-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-micronodelist-field-native", true, false, "draft")
			);
	}

	@Parameters(name = "query={0},version={3},apiVersion={5}")
	public static Collection<Object[]> paramData() {
		return Stream.of(GraphQLEndpointTest.queries(), JavaGraphQLEndpointTest.queries(), NativeGraphQLEndpointTest.queries())
				.flatMap(java.util.function.Function.identity())
				.flatMap(testCase -> IntStream.rangeClosed(1, CURRENT_API_VERSION)
				.mapToObj(version -> {
					// Make sure all testData entries have six parts.
					Object[] array = testCase.toArray(new Object[6]);
					array[5] = "v" + version;
					return array;
				})).collect(Collectors.toList());
	}

	@Before
	public void setUp() throws Exception {
		this.client = client(apiVersion);
	}

	@Test
	public void testNodeQuery() throws Exception {
		String microschemaUuid = null;
		if (withMicroschema) {
			// 1. Create the microschema
			MicroschemaCreateRequest microschemaRequest = new MicroschemaCreateRequest();
			microschemaRequest.setName("TestMicroschema");
			microschemaRequest.addField(FieldUtil.createStringFieldSchema("text"));
			microschemaRequest.addField(FieldUtil.createNodeFieldSchema("nodeRef").setAllowedSchemas("content"));
			microschemaRequest.addField(FieldUtil.createListFieldSchema("nodeList", "node"));
			MicroschemaResponse microschemaResponse = call(() -> client.createMicroschema(microschemaRequest));
			microschemaUuid = microschemaResponse.getUuid();
			call(() -> client.assignMicroschemaToProject(PROJECT_NAME, microschemaResponse.getUuid()));
		} else {
			try (Tx tx = tx()) {
				for (HibMicroschema microschema : tx.microschemaDao().findAll()) {
					tx.microschemaDao().delete(microschema, new DummyBulkActionContext());
				}
				tx.success();
			}
		}

		// update branch
		if (withBranchPathPrefix) {
			call(() -> client.updateBranch(PROJECT_NAME, initialBranchUuid(),
					new BranchUpdateRequest().setHostname("getmesh.io").setSsl(false).setPathPrefix("/base/path")));
		}

		try (Tx tx = tx()) {
			MicroschemaDao microschemaDao = tx.microschemaDao();

			HibNode node = folder("2015");
			HibNode folder = folder("news");
			tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(folder, "de"), initialBranchUuid(), null);
			tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(folder, "de"), initialBranchUuid(), null);

			HibNode node2 = content();
			tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(node2, "en"), initialBranchUuid(), null);
			tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(node2, "de"), initialBranchUuid(), null);
			HibNode node3 = folder("2014");

			// Update the folder schema to contain all fields
			HibSchema schemaContainer = schemaContainer("folder");
			SchemaVersionModel schema = schemaContainer.getLatestVersion().getSchema();
			schema.setUrlFields("niceUrl");
			schema.setAutoPurge(true);
			NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
			nodeFieldSchema.setName("nodeRef");
			nodeFieldSchema.setLabel("Some label");
			nodeFieldSchema.setAllowedSchemas("folder");
			schema.addField(nodeFieldSchema);

			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName("binary");
			schema.addField(binaryFieldSchema);

			S3BinaryFieldSchemaImpl s3BinaryFieldSchema = new S3BinaryFieldSchemaImpl();
			s3BinaryFieldSchema.setName("s3Binary");
			schema.addField(s3BinaryFieldSchema);

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

			HtmlFieldSchema emptyLinkFieldSchema = new HtmlFieldSchemaImpl();
			emptyLinkFieldSchema.setName("emptyLink");
			schema.addField(emptyLinkFieldSchema);

			StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName("string");
			schema.addField(stringFieldSchema);

			StringFieldSchema niceUrlFieldSchema = new StringFieldSchemaImpl();
			niceUrlFieldSchema.setName("niceUrl");
			schema.addField(niceUrlFieldSchema);

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

			if (withMicroschema) {
				MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
				micronodeFieldSchema.setAllowedMicroSchemas("vcard");
				micronodeFieldSchema.setName("micronode");
				schema.addField(micronodeFieldSchema);
			}
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(schemaContainer("folder").getLatestVersion());

			// Setup some test data
			HibNodeFieldContainer container = tx.contentDao().createFieldContainer(node, "en", initialBranch(), user());

			// node
			container.createNode("nodeRef", node2);

			// number
			container.createNumber("number").setNumber(42.1);

			// date
			long milisec = dateToMilis(DATES.get(0));
			container.createDate("date").setDate(milisec);

			// html
			container.createHTML("html").setHtml("some html");

			// htmlLink
			container.createHTML("htmlLink").setHtml("Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

			// emptyLink
			container.createHTML("emptyLink").setHtml("Link to nowhere: {{mesh.link(\"00000000000000000000000000000000\", \"en\")}}");

			// string
			container.createString("string").setString("some string");

			// niceUrl
			container.createString("niceUrl").setString("/some/url");

			// stringLink
			container.createString("stringLink").setString("Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

			// boolean
			container.createBoolean("boolean").setBoolean(true);

			// binary
			HibBinary binary = tx.binaries().create("hashsumvalue", 1L).runInExistingTx(tx);
			binary.setImageHeight(10).setImageWidth(20).setSize(2048);
			container.createBinary("binary", binary).setImageDominantColor("00FF00")
				.setImageFocalPoint(new FocalPoint(0.2f, 0.3f)).setMimeType("image/jpeg");

			// s3binary
			S3HibBinary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), node.getUuid() + "/s3", "test.jpg").runInExistingTx(tx);
			container.createS3Binary("s3Binary", s3binary).setImageDominantColor("00FF00");

			// stringList
			HibStringFieldList stringList = container.createStringList("stringList");
			stringList.createString("A");
			stringList.createString("B");
			stringList.createString("C");
			stringList.createString("D Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

			// htmlList
			HibHtmlFieldList htmlList = container.createHTMLList("htmlList");
			htmlList.createHTML("A");
			htmlList.createHTML("B");
			htmlList.createHTML("C");
			htmlList.createHTML("D Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

			// dateList
			HibDateFieldList dateList = container.createDateList("dateList");
			dateList.createDate(dateToMilis(DATES.get(0)));
			dateList.createDate(dateToMilis(DATES.get(1)));
			dateList.createDate(dateToMilis(DATES.get(2)));

			// numberList
			HibNumberFieldList numberList = container.createNumberList("numberList");
			numberList.createNumber(42L);
			numberList.createNumber(1337);
			numberList.createNumber(0.314f);

			// booleanList
			HibBooleanFieldList booleanList = container.createBooleanList("booleanList");
			booleanList.createBoolean(true);
			booleanList.createBoolean(null);
			booleanList.createBoolean(false);

			// nodeList
			HibNodeFieldList nodeList = container.createNodeList("nodeList");
			nodeList.createNode(0, node2);
			nodeList.createNode(1, node3);

			if (withMicroschema) {
				// micronodeList
				HibMicronodeFieldList micronodeList = container.createMicronodeList("micronodeList");
				HibMicronode firstMicronode = micronodeList.createMicronode(microschemaContainer("vcard").getLatestVersion());
				firstMicronode.createString("firstName").setString("Joe");
				firstMicronode.createString("lastName").setString("Doe");
				firstMicronode.createString("address").setString("Somewhere");
				firstMicronode.createString("postcode").setString("1010");

				HibMicronode secondMicronode = micronodeList.createMicronode(microschemaDao.findByUuid(microschemaUuid).getLatestVersion());
				secondMicronode.createString("text").setString("Joe");
				secondMicronode.createNode("nodeRef", content());
				HibNodeFieldList micrnodeNodeList = secondMicronode.createNodeList("nodeList");
				micrnodeNodeList.createNode(0, node2);
				micrnodeNodeList.createNode(1, node3);

				// micronode
				HibMicronodeField micronodeField = container.createMicronode("micronode", microschemaContainer("vcard").getLatestVersion());
				micronodeField.getMicronode().createString("firstName").setString("Joe");
				micronodeField.getMicronode().createString("lastName").setString("Doe");
				micronodeField.getMicronode().createString("address").setString("Somewhere");
				micronodeField.getMicronode().createString("postcode").setString("1010");
			}
			tx.contentDao().updateWebrootPathInfo(container, initialBranchUuid(), null);
			tx.success();
		}

		// Publish all nodes
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		call(() -> client.publishNode(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));

		// Create a draft node
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page"));
		request.setParentNode(new NodeReference().setUuid(baseNodeUuid));
		call(() -> client.createNode(PROJECT_NAME, request));

		// Create a node which contains mesh links
		createLanguageLinkResolvingNode(NODE_WITH_LINKS_UUID, baseNodeUuid, CONTENT_UUID).blockingAwait();

		// Create referencing node (en)
		NodeCreateRequest request2 = new NodeCreateRequest();
		request2.setLanguage("en");
		request2.setSchema(new SchemaReferenceImpl().setName("folder"));
		request2.getFields().put("nodeRef", FieldUtil.createNodeField(NODE_WITH_LINKS_UUID));
		request2.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_WITH_LINKS_UUID));
		request2.getFields().put("slug", FieldUtil.createStringField("node-with-reference-en"));
		request2.setParentNode(new NodeReference().setUuid(NEWS_UUID));
		call(() -> client.createNode(NODE_WITH_NODE_REF_UUID, PROJECT_NAME, request2));

		// Create referencing node content (de)
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.setVersion("0.1");
		nodeUpdateRequest.getFields().put("nodeRef", FieldUtil.createNodeField(NODE_WITH_LINKS_UUID));
		nodeUpdateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_WITH_LINKS_UUID));
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("node-with-reference-de"));
		call(() -> client.updateNode(PROJECT_NAME, NODE_WITH_NODE_REF_UUID, nodeUpdateRequest));

		// Add node reference to user
		UserResponse user = call(() -> client.me());
		NodeResponse newsNode = call(() -> client.findNodeByUuid(PROJECT_NAME, NEWS_UUID));
		call(() -> client.updateUser(user.getUuid(), new UserUpdateRequest().setNodeReference(newsNode)));

		// Now execute the query and assert it
		String query = getGraphQLQuery(queryName, apiVersion).replace("%" + SCHEMA_UUID + "%", schemaContainer("folder").getUuid());
		//System.out.println(query);
		GraphQLResponse response = call(
			() -> client.graphqlQuery(PROJECT_NAME, query, new VersioningParametersImpl().setVersion(version)));
		JsonObject jsonResponse = new JsonObject(response.toJson());
		//System.out.println(jsonResponse.encodePrettily());
		if (assertion == null) {
			assertThat(jsonResponse)
					.replacingPlaceholderVariable(SCHEMA_UUID, schemaContainer("folder").getUuid())
					.compliesToAssertions(queryName, apiVersion);
		} else {
			assertion.accept(jsonResponse);
		}
	}

	protected long dateToMilis(String date)  {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
		var parsedDateTime = dateTime.atOffset(ZoneOffset.UTC);

		return parsedDateTime.toInstant().toEpochMilli();
	}

	protected Completable createLanguageLinkResolvingNode(String nodeUuid, String parentUuid, String referencedUuid) throws Exception {

		Function<String, FieldMap> createFields = language -> {
			FieldMap map = new FieldMapImpl();

			// stringList
			StringFieldListImpl stringList = new StringFieldListImpl();
			stringList.add("A Link: {{mesh.link(\"" + referencedUuid + "\", \"en\")}}");
			stringList.add("B Link: {{mesh.link(\"" + referencedUuid + "\", \"de\")}}");
			stringList.add("C Link: {{mesh.link(\"" + referencedUuid + "\")}}");
			map.put("stringList", stringList);

			// htmlList
			HtmlFieldListImpl htmlList = new HtmlFieldListImpl();
			htmlList.add("A Link: {{mesh.link(\"" + referencedUuid + "\", \"en\")}}");
			htmlList.add("B Link: {{mesh.link(\"" + referencedUuid + "\", \"de\")}}");
			htmlList.add("C Link: {{mesh.link(\"" + referencedUuid + "\")}}");
			map.put("htmlList", htmlList);

			// html
			HtmlField htmlField = new HtmlFieldImpl().setHTML("HTML Link: {{mesh.link(\"" + referencedUuid + "\")}}");
			map.put("html", htmlField);

			// string
			StringField stringField = new StringFieldImpl().setString("String Link: {{mesh.link(\"" + referencedUuid + "\")}}");
			map.put("string", stringField);

			map.put("slug", new StringFieldImpl().setString("new-page-" + language));
			return map;
		};

		Function<NodeResponse, Single<NodeResponse>> updateNode = response -> {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setFields(createFields.apply("de"));
			updateRequest.setLanguage("de");
			return client.updateNode(PROJECT_NAME, response.getUuid(), updateRequest).toSingle();
		};

		NodeCreateRequest createRequest = new NodeCreateRequest();
		createRequest.setLanguage("en");
		createRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
		createRequest.setParentNode(new NodeReference().setUuid(parentUuid));
		createRequest.setFields(createFields.apply("en"));

		return client.createNode(nodeUuid, PROJECT_NAME, createRequest).toSingle()
			.flatMap(updateNode)
			.ignoreElement();
	}

	/**
	 * Special assertion for the <code>node/link/children</code> test query.
	 *
	 * <p>
	 * This asserts that the children of certain elements each contain two german and to english nodes respectively,
	 * and that the language of the loaded node is german.
	 * </p>
	 *
	 * <p>
	 * The special assertions are used because the order of children is not deterministic and the default
	 * {@link JsonObjectAssert#compliesToAssertions(String) assertion} can randomly fail.
	 * </p>
	 *
	 * @param result The JSON object from the GraphQL response
	 */
	protected static void checkNodeLinkChildrenResponse(JsonObject result) {
		Collector<Object, ?, Map<String, List<Object>>> groupByLanguage = Collectors.groupingBy(o -> ((JsonObject) o).getString("language"));
		System.out.println(result.encodePrettily());
		JsonObject node = result.getJsonObject("data").getJsonObject("node");

		assertThat(node.getString("language")).as("Node language").isEqualTo("de");

		Map<String, List<Object>> childCount = node.getJsonObject("c1").getJsonArray("elements").stream().collect(groupByLanguage);

		assertThat(childCount.get("de").size()).as("German children of c1").isEqualTo(2);
		assertThat(childCount.get("en").size()).as("English children of c1").isEqualTo(2);

		childCount = node.getJsonObject("c2").getJsonArray("elements").stream().collect(groupByLanguage);
		assertThat(childCount.get("de").size()).as("German children of c2").isEqualTo(2);
		assertThat(childCount.get("en").size()).as("English children of c2").isEqualTo(2);

		JsonArray hipChildren = node.getJsonObject("hip").getJsonArray("elements");

		IntStream.range(0, hipChildren.size())
			.mapToObj(idx -> hipChildren.getJsonObject(idx).put("idx", idx))
			.forEach(c ->  {
				Map<String, List<Object>> count = c.getJsonObject("parent").getJsonObject("children").getJsonArray("elements").stream().collect(groupByLanguage);

				assertThat(count.get("de").size()).as("{} german children of hip", c.getInteger("idx")).isEqualTo(2);
				assertThat(count.get("en").size()).as("{} english children of hip", c.getInteger("idx")).isEqualTo(2);
			});
	}

	/**
	 * Special assertion for the <code>node-languages-query</code>.
	 *
	 * @param result
	 */
	protected static void checkNodeLanguageContent(JsonObject result) {
		JsonArray languages = result.getJsonObject("data").getJsonObject("node").getJsonArray("languages");
		assertThat(languages).containsJsonObjectHashesInAnyOrder(
			obj -> hash(
				obj.getString("language"),
				obj.getString("path")
			),
			hash("en", "/News/2015/News_2015.en.html"),
			hash("de", "/Neuigkeiten/2015/News_2015.de.html")
		);
	}
}

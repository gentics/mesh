package com.gentics.mesh.plugin.mustread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.controller.SchemaController;
import com.gentics.mesh.plugin.mustread.helper.JsonHelper;
import com.gentics.mesh.plugin.mustread.object.User;
import com.gentics.mesh.plugin.mustread.plugin.MustReadTestPlugin;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;

public class MustReadPluginTest extends AbstractPluginLocalIntegrationTest implements MustReadPluginTestHelper {

	private static final String testDir = "target/test-must-read-plugin";

	@ClassRule
	public static final MeshLocalServer server = new HibernateMeshLocalServer()
		.withPlugin(MustReadTestPlugin.class, API_NAME)
		.withNodeName("test-mesh")
		.waitForStartup();

	private static final String folderSchemaName = "must_read_test_folder";
	private static final String pageSchemaName = "must_read_test_page";
	private static final String nameFieldName = "name";
	private static final String mustReadFieldName = "must_read";
	private static final String firstFolderName = String.format("folder_first_%s", System.currentTimeMillis());
	private static final String secondFolderName = String.format("folder_second_%s", System.currentTimeMillis());
	private static final String unreadFirstPageName = String.format("unread_%s_first_page", System.currentTimeMillis());
	private static final String unreadSecondPageName = String.format("unread_%s_second_page", System.currentTimeMillis());
	private static final String readFirstPageName = String.format("read_%s_first_page", System.currentTimeMillis());
	private static final String readSecondPageName = String.format("read_%s_second_page", System.currentTimeMillis());
	private static final String notUsedFirstPageName = String.format("notUsed_%s_first_page", System.currentTimeMillis());
	private static final String notUsedSecondPageName = String.format("notUsed_%s_second_page", System.currentTimeMillis());
	private User user;

	@BeforeClass
	public static void initializeAllTestData() throws InterruptedException {
		PluginTestUtil.waitForPluginRegistration(server, 5_000, API_NAME);
		for (ProjectConfig project : MustReadTestPlugin.generateTestConfiguration().getProjects()) {
			createMeshProject(project.getName());
		}

		createMeshProject("NoConfig");
		Thread.sleep(15000);

	}

	@AfterClass
	public static void cleanup() throws IOException {
		File testDir = new File(MustReadPluginTest.testDir);
		File configFile;

		try {
			configFile = new File(MustReadPluginTest.class.getResource("config.yml").toURI());
		} catch (Exception e) {
			configFile = new File("config.yml");
		}

		if (testDir.exists()) {
			FileUtils.forceDelete(testDir);
		}

		if (configFile.exists()) {
			FileUtils.forceDelete(configFile);
		}

	}

	private static void createMeshProject(String name) {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchemaRef("folder");
		server.client().createProject(request).toSingle().subscribe();
	}

	private static MeshOptions createMeshOptions() {
		MeshOptions options = OptionsLoader.createOrloadOptions(HibernateMeshOptions.class);

		options.setPluginDirectory(testDir);
		options.getAuthenticationOptions().setKeystorePassword("ABC");

		return options;
	}

	@Before
	public void initializeTestData() throws IOException {
		if (user == null) {
			user = User.parse(getResponseJsonGetRequest("/api/v1/auth/me"));
		}
	}

	public void debugOutput() {
		System.out.println(String.format("Port: %s", server.getPort()));
		System.out.println(String.format("Token: %s", server.client().getAuthentication().getToken()));
		System.out.println(String.format("UserUUID: %s", user.getUUID()));
	}

	@Test
	public void testConfiguration() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		String jsonString = getResponseTextGetRequest("/api/v2/plugins/" + API_NAME + "/configuration");

		assertEquals(mapper.writeValueAsString(MustReadTestPlugin.generateTestConfiguration()), jsonString);
	}

	@Test
	public void testSchema() {
		assertTrue(String.format("Schema '%s' doesn't exists", SchemaController.folderSchemaName),
			existsSchema(SchemaController.folderSchemaName, null));
		assertTrue(String.format("Schema '%s' doesn't exists", SchemaController.userSchemaName), existsSchema(SchemaController.userSchemaName, null));
		assertTrue(String.format("Schema '%s' doesn't exists", SchemaController.nodeSchemaName), existsSchema(SchemaController.nodeSchemaName, null));

		for (ProjectConfig project : MustReadTestPlugin.generateTestConfiguration().getProjects()) {
			assertTrue(String.format("Schema '%s' is not assigned to %s", SchemaController.folderSchemaName, project.getName()),
				existsSchema(SchemaController.folderSchemaName, project.getName()));
			assertTrue(String.format("Schema '%s' is not assigned to %s", SchemaController.userSchemaName, project.getName()),
				existsSchema(SchemaController.userSchemaName, project.getName()));
			assertTrue(String.format("Schema '%s' is not assigned to %s", SchemaController.nodeSchemaName, project.getName()),
				existsSchema(SchemaController.nodeSchemaName, project.getName()));
		}
	}

	private boolean existsSchema(String schemaName, String projectName) {
		SchemaListResponse schemas;

		if (StringUtils.isNotEmpty(projectName)) {
			schemas = server.client().findSchemas(projectName).toSingle().blockingGet();
		} else {
			schemas = server.client().findSchemas().toSingle().blockingGet();
		}

		Optional<SchemaResponse> optSingleSchema = schemas.getData().stream().filter(s -> s.getName().equals(schemaName)).findFirst();

		return optSingleSchema.isPresent();
	}

	@Test
	public void testFolders() throws IOException {
		for (ProjectConfig project : MustReadTestPlugin.generateTestConfiguration().getProjects()) {
			String url = String.format("/api/v2/%s/webroot/%s", project.getName(), project.getFolderName());
			assertEquals(String.format("Error checking '%s'(%s) in '%s'", project.getFolderName(), url, project.getName()), 200,
				getResponseCodeGetRequest(url));
		}
	}

	@Test
	public void testRead() throws IOException {
		List<String> nodes = new ArrayList<>();
		nodes.add("test");

		JsonObject emptyData = new JsonObject()
			.put("nodes", new ArrayList<String>());
		JsonObject normalData = new JsonObject()
			.put("nodes", nodes);

		assertEquals("Empty GraphQl didn't return 501", HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodePostRequest("/api/v2/EmptyGraphQl/plugins/" + API_NAME + "/read", normalData));
		assertEquals("Empty folder name didn't return 501", HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodePostRequest("/api/v2/EmptyFolderName/plugins/" + API_NAME + "/read", normalData));
		assertEquals("Non Existing UUID Query File didn't return 501", HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodePostRequest("/api/v2/NonExistingUUIDQueryFile/plugins/" + API_NAME + "/read", normalData));
		assertEquals("Non Existing Data Query File didn't return 501", HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodePostRequest("/api/v2/NonExistingDataQueryFile/plugins/" + API_NAME + "/read", normalData));
		assertEquals("Test with no data didn't return 400", HttpResponseStatus.BAD_REQUEST.code(),
			getResponseCodePostRequest("/api/v2/Test/plugins/" + API_NAME + "/read", emptyData));
		assertEquals("No UUID set didn't return 200", HttpResponseStatus.OK.code(),
			getResponseCodePostRequest("/api/v2/NoSetUUIDFieldName/plugins/" + API_NAME + "/read", normalData));

		testRead(readFirstPageName, readSecondPageName);
	}

	private void testRead(String firstId, String secondId) throws IOException {
		List<String> nodes = new ArrayList<>();

		nodes.add(firstId);
		nodes.add(secondId);
		JsonObject testData = new JsonObject()
			.put("nodes", nodes);

		ProjectConfig project = MustReadTestPlugin.generateTestConfiguration().getProjectConfiguration("Test");

		assertEquals("Test didn't return 200", 200, getResponseCodePostRequest("/api/v2/Test/plugins/" + API_NAME + "/read", testData));

		String firstUrl = String.format("/api/v2/%s/webroot/%s/%s/%s", project.getName(), project.getFolderName(), user.getUUID(), firstId);
		String secondUrl = String.format("/api/v2/%s/webroot/%s/%s/%s", project.getName(), project.getFolderName(), user.getUUID(), secondId);

		assertEquals(String.format("First node (%s) not marked", firstId), 200, getResponseCodeGetRequest(firstUrl));
		assertEquals(String.format("Second node (%s) not marked", secondId), 200, getResponseCodeGetRequest(secondUrl));
	}

	@Test
	public void testUnread() throws IOException {
		List<String> expected = new ArrayList<>();
		List<String> ids = prepareMeshDataForUnreadTest();

		testRead(ids.get(0), ids.get(1));

		expected.add(unreadFirstPageName);
		expected.add(unreadSecondPageName);

		assertEquals(HttpResponseStatus.NOT_IMPLEMENTED.code(), getResponseCodeGetRequest("/api/v2/EmptyGraphQl/plugins/" + API_NAME + "/unread"));
		assertEquals(HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodeGetRequest("/api/v2/NonExistingUUIDQueryFile/plugins/" + API_NAME + "/unread"));
		assertEquals(HttpResponseStatus.NOT_IMPLEMENTED.code(),
			getResponseCodeGetRequest("/api/v2/NonExistingDataQueryFile/plugins/" + API_NAME + "/unread"));
		JsonObject response = getResponseJsonGetRequest("/api/v2/Test/plugins/" + API_NAME + "/unread");
		List<String> allFound = new ArrayList<>();

		JsonHelper.fetchKeys(response, "name", false, "", allFound::add);

		assertEquals("There were not exactly 2 entries found", 2, allFound.size());
		assertTrue("Not all expected unread entries were found", allFound.containsAll(expected));

		assertEquals(HttpResponseStatus.NOT_IMPLEMENTED.code(), getResponseCodeGetRequest("/api/v2/NoConfig/plugins/" + API_NAME + "/unread"));
		assertEquals(HttpResponseStatus.OK.code(), getResponseCodeGetRequest("/api/v2/NoSetUUIDFieldName/plugins/" + API_NAME + "/unread"));
	}

	private List<String> prepareMeshDataForUnreadTest() {
		List<String> createdIds = new ArrayList<>();

		SchemaResponse folderSchema = server.client().createSchema(getFolderSchemaCreateRequest()).toSingle().doOnSuccess((response -> {
			server.client().assignSchemaToProject("Test", response.getUuid()).toCompletable().blockingGet();
			server.client().assignSchemaToProject("NoSetUUIDFieldName", response.getUuid()).toCompletable().blockingGet();
		})).blockingGet();
		SchemaResponse pageSchema = server.client().createSchema(getPageSchemaCreateRequest()).toSingle().doOnSuccess((response -> {
			server.client().assignSchemaToProject("Test", response.getUuid()).toCompletable().blockingGet();
			server.client().assignSchemaToProject("NoSetUUIDFieldName", response.getUuid()).toCompletable().blockingGet();
		})).blockingGet();

		String firstFolderPath = String.format("/%s", firstFolderName);
		String secondFolderPath = String.format("/%s", secondFolderName);

		server.client().webrootCreate("Test", firstFolderPath, getNodeCreateRequest(firstFolderName, folderSchema.toReference())).blockingGet();
		server.client().webrootCreate("Test", secondFolderPath, getNodeCreateRequest(secondFolderName, folderSchema.toReference())).blockingGet();

		createdIds.add(server.client().webrootCreate("Test", String.format("%s/%s", firstFolderPath, readFirstPageName),
			getNodeCreateRequest(readFirstPageName, pageSchema.toReference(), true)).blockingGet().getUuid());
		createdIds.add(server.client().webrootCreate("Test", String.format("%s/%s", secondFolderPath, readSecondPageName),
			getNodeCreateRequest(readSecondPageName, pageSchema.toReference(), true)).blockingGet().getUuid());

		server.client().webrootCreate("Test", String.format("%s/%s", firstFolderPath, unreadFirstPageName),
			getNodeCreateRequest(unreadFirstPageName, pageSchema.toReference(), true)).blockingGet();
		server.client().webrootCreate("Test", String.format("%s/%s", secondFolderPath, unreadSecondPageName),
			getNodeCreateRequest(unreadSecondPageName, pageSchema.toReference(), true)).blockingGet();
		server.client().webrootCreate("Test", String.format("%s/%s", firstFolderPath, notUsedFirstPageName),
			getNodeCreateRequest(notUsedFirstPageName, pageSchema.toReference(), false)).blockingGet();
		server.client().webrootCreate("Test", String.format("%s/%s", secondFolderPath, notUsedSecondPageName),
			getNodeCreateRequest(notUsedSecondPageName, pageSchema.toReference(), false)).blockingGet();

		return createdIds;
	}

	private SchemaCreateRequest getFolderSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		List<FieldSchema> fields = new ArrayList<>();

		fields.add(new StringFieldSchemaImpl().setName(nameFieldName).setLabel("Name of the test node").setRequired(true));

		request.setName(folderSchemaName);
		request.setFields(fields);
		request.setContainer(true);
		request.setDescription("Test schema for a folder");
		request.setDisplayField(nameFieldName);
		request.setSegmentField(nameFieldName);

		return request;
	}

	private SchemaCreateRequest getPageSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		List<FieldSchema> fields = new ArrayList<>();

		fields.add(new StringFieldSchemaImpl().setName(nameFieldName).setLabel("Name of the test node").setRequired(true));
		fields.add(new BooleanFieldSchemaImpl().setName(mustReadFieldName).setLabel("This test node must be read").setRequired(false));

		request.setName(pageSchemaName);
		request.setFields(fields);
		request.setContainer(false);
		request.setDescription("Test schema for a page");
		request.setDisplayField(nameFieldName);
		request.setSegmentField(nameFieldName);

		return request;
	}

	private NodeCreateRequest getNodeCreateRequest(String nodeName, SchemaReference schemaReference, boolean mustRead) {
		NodeCreateRequest request = getNodeCreateRequest(nodeName, schemaReference);
		FieldMap fields = request.getFields();

		fields.put(mustReadFieldName, new BooleanFieldImpl().setValue(mustRead));

		request.setFields(fields);

		return request;
	}

	private NodeCreateRequest getNodeCreateRequest(String nodeName, SchemaReference schemaReference) {
		NodeCreateRequest request = new NodeCreateRequest();
		FieldMap fields = new FieldMapImpl();

		fields.put(nameFieldName, new StringFieldImpl().setString(nodeName));

		request.setSchema(schemaReference);
		request.setFields(fields);
		request.setLanguage("en");

		return request;
	}

	@Override
	public MeshRestClient meshClient() {
		return server.client();
	}

	@Override
	public String serverURL() {
		return "http://" + server.getHostname() + ":" + server.getPort();
	}
}

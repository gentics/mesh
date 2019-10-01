package com.gentics.mesh.plugin.mustread;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.plugin.PluginConfigUtil;
import com.gentics.mesh.plugin.mustread.config.FieldNamesConfig;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.config.QueryFilesConfig;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshDockerServer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MustReadPluginIT extends AbstractPluginTest {

	public static final File PLUGINS_DIR = new File("target", "it_" + System.currentTimeMillis());

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		try {
			setupPlugin();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void setupPlugin() throws IOException {
		PLUGINS_DIR.deleteOnExit();
		PLUGINS_DIR.mkdirs();
		File mustReadDir = new File(PLUGINS_DIR, "mustread");
		mustReadDir.mkdirs();

		// 1. Create Plugin Config
		MustReadConfig pluginConfig = new MustReadConfig();
		pluginConfig.setNodeListParameterName("uuids");
		ProjectConfig projectConfig = new ProjectConfig();
		projectConfig.setName("Test");
		projectConfig.setFolderName("Must-Read");
		projectConfig.setQueryFiles(new QueryFilesConfig().setUuid("MustRead.graphql").setData("Data.graphql"));
		projectConfig.setFieldNames(new FieldNamesConfig().setUuid("testUuid").setPublished("testPublished"));
		pluginConfig.setProjects(Arrays.asList(projectConfig));
		PluginConfigUtil.writeConfig(new File(mustReadDir, "config.yml"), pluginConfig);
		File storageDir = new File(mustReadDir, "storage");

		// 2. Create graphql files
		File gqlDir = new File(storageDir, "graphql");
		FileUtils.copyInputStreamToFile(MustReadPluginIT.class.getResourceAsStream("/it/Data.graphql"), new File(gqlDir, "Data.graphql"));
		FileUtils.copyInputStreamToFile(MustReadPluginIT.class.getResourceAsStream("/it/MustRead.graphql"), new File(gqlDir, "MustRead.graphql"));
		gqlDir.mkdirs();

		// 3. Copy pre-build plugin jar
		Path pluginJar = Files.list(Paths.get("target"))
			.filter(Files::isRegularFile)
			.filter(f -> !f.getFileName().toString().endsWith("sources.jar"))
			.filter(f -> !f.getFileName().toString().startsWith("original-"))
			.findFirst().orElse(null);
		if (pluginJar == null) {
			fail("Could not find plugin jar in target folder. Make sure you run mvn package before running integration tests");
		}
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, "mustread-plugin.jar"));

	}

	@AfterClass
	public static void cleanup() throws IOException {
		if (PLUGINS_DIR.exists()) {
			FileUtils.forceDelete(PLUGINS_DIR);
		}
	}

	@ClassRule
	public static final MeshDockerServer meshServer = new MeshDockerServer("gentics/mesh:0.40.2", Vertx.vertx())
		.withFileSystemBind(PLUGINS_DIR.getAbsolutePath(), "/plugins")
		.waitForStartup();

	@Test
	public void testIt() throws IOException, InterruptedException {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test");
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = meshServer.client().createProject(request).blockingGet();

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setParentNodeUuid(projectResponse.getRootNode().getUuid());
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("testnode"));
		String nodeUuid = "2c572ab6f65e4f42972ab6f65ebf4245";
		NodeResponse nodeResponse = meshServer.client().createNode(nodeUuid, "Test", nodeCreateRequest).blockingGet();

		Thread.sleep(2000);
		JsonObject response2 = getResponseJsonGetRequest("/api/v2/admin/plugins");
		System.out.println(response2.encodePrettily());
		JsonObject response = getResponseJsonGetRequest("/api/v2/Test/plugins/" + API_NAME + "/unread");
		System.out.println(response.encodePrettily());

		JsonObject testData = new JsonObject();
		testData.put("uuids", new JsonArray().add(nodeUuid));
		assertEquals(200, getResponseCodePostRequest("/api/v2/Test/plugins/" + API_NAME + "/read", testData));

		JsonObject response3 = getResponseJsonGetRequest("/api/v2/Test/plugins/" + API_NAME + "/unread");
		System.out.println(response3.encodePrettily());
	}

	@Override
	protected MeshRestClient meshClient() {
		return meshServer.client();
	}

	@Override
	protected String serverURL() {
		return "http://" + meshServer.getHostname() + ":" + meshServer.getPort();
	}
}

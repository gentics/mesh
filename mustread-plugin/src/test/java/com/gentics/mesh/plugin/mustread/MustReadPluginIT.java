package com.gentics.mesh.plugin.mustread;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

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
import com.gentics.mesh.plugin.test.common.AbstractPluginContainerIntegrationTest;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MustReadPluginIT extends AbstractPluginContainerIntegrationTest implements MustReadPluginTestHelper {

	@BeforeClass
	public static void setUp() throws Exception {
		copyPlugin("mustread", pluginDir -> {
			// 1. Create Plugin Config
			MustReadConfig pluginConfig = new MustReadConfig();
			pluginConfig.setNodeListParameterName("uuids");
			ProjectConfig projectConfig = new ProjectConfig();
			projectConfig.setName("Test");
			projectConfig.setFolderName("Must-Read");
			projectConfig.setQueryFiles(new QueryFilesConfig().setUuid("MustRead.graphql").setData("Data.graphql"));
			projectConfig.setFieldNames(new FieldNamesConfig().setUuid("testUuid").setPublished("testPublished"));
			pluginConfig.setProjects(Arrays.asList(projectConfig));
			PluginConfigUtil.writeConfig(new File(pluginDir, "config.yml"), pluginConfig);
			File storageDir = new File(pluginDir, "storage");

			// 2. Create graphql files
			File gqlDir = new File(storageDir, "graphql");
			FileUtils.copyInputStreamToFile(MustReadPluginIT.class.getResourceAsStream("/it/Data.graphql"), new File(gqlDir, "Data.graphql"));
			FileUtils.copyInputStreamToFile(MustReadPluginIT.class.getResourceAsStream("/it/MustRead.graphql"), new File(gqlDir, "MustRead.graphql"));
			gqlDir.mkdirs();
		});

	}

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
	public MeshRestClient meshClient() {
		return meshServer.client();
	}

	@Override
	public String serverURL() {
		return "http://" + meshServer.getHost() + ":" + meshServer.getPort();
	}
}

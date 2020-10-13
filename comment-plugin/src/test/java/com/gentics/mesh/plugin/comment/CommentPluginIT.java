package com.gentics.mesh.plugin.comment;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.plugin.PluginConfigUtil;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.test.common.AbstractPluginContainerIntegrationTest;
import com.gentics.mesh.rest.client.MeshRestClient;

public class CommentPluginIT extends AbstractPluginContainerIntegrationTest {

	public static final String API_NAME = "comment";

	@BeforeClass
	public static void setUp() throws Exception {

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
		meshServer.client().createNode(nodeUuid, "Test", nodeCreateRequest).blockingGet();

		copyPlugin("comment", pluginDir -> {

			// 1. Create Plugin Config
			Configuration pluginConfig = new Configuration();
			pluginConfig.setProjectNames(Arrays.asList("Test"));
			pluginConfig.setAllowAnonymous(false);
			pluginConfig.setRequiresApproval(false);
			pluginConfig.setCommentRoot("/comments");
			pluginConfig.setSchemaPrefix("test");

			PluginConfigUtil.writeConfig(new File(pluginDir, "config.yml"), pluginConfig);
			File storageDir = new File(pluginDir, "storage");

			// 2. Create templates
			File templatesDir = new File(storageDir, "templates");
			templatesDir.mkdirs();
			FileUtils.copyInputStreamToFile(CommentPluginIT.class.getResourceAsStream("/storage/templates/list.hbs"),
				new File(templatesDir, "list.hbs"));
		});
	}

	@Test
	public void testPluginDeployment() {
		// NOP
	}

	protected MeshRestClient meshClient() {
		return meshServer.client();
	}

	protected String serverURL() {
		return "http://" + meshServer.getHost() + ":" + meshServer.getPort();
	}
}

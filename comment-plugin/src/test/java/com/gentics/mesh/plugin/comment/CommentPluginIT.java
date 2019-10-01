package com.gentics.mesh.plugin.comment;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.plugin.PluginConfigUtil;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshDockerServer;

import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class CommentPluginIT {

	public static final File PLUGINS_DIR = new File("target", "it_" + System.currentTimeMillis());

	public static final String API_NAME = "comment";

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
		File commentDir = new File(PLUGINS_DIR, "comment");
		commentDir.mkdirs();

		// 1. Create Plugin Config
		Configuration pluginConfig = new Configuration();
		pluginConfig.setProjectNames(Arrays.asList("Test"));
		pluginConfig.setAllowAnonymous(false);
		pluginConfig.setRequiresApproval(false);
		pluginConfig.setCommentRoot("/comments");
		pluginConfig.setSchemaPrefix("test");

		PluginConfigUtil.writeConfig(new File(commentDir, "config.yml"), pluginConfig);
		File storageDir = new File(commentDir, "storage");

		// 2. Create templates
		File templatesDir = new File(storageDir, "templates");
		templatesDir.mkdirs();
		FileUtils.copyInputStreamToFile(CommentPluginIT.class.getResourceAsStream("/storage/templates/list.hbs"),
			new File(templatesDir, "list.hbs"));
	}

	private static void copyJar() throws IOException {
		// 3. Copy pre-build plugin jar
		Path pluginJar = Files.list(Paths.get("target"))
			.filter(Files::isRegularFile)
			.filter(f -> !f.getFileName().toString().endsWith("sources.jar"))
			.filter(f -> !f.getFileName().toString().startsWith("original-"))
			.findFirst().orElse(null);
		if (pluginJar == null) {
			fail("Could not find plugin jar in target folder. Make sure you run mvn package before running integration tests");
		}
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, "comment-plugin.jar"));

	}

	@AfterClass
	public static void cleanup() throws IOException {
		if (PLUGINS_DIR.exists()) {
			// FileUtils.forceDelete(PLUGINS_DIR);
		}
	}

	@ClassRule
	public static final MeshDockerServer meshServer = new MeshDockerServer("gentics/mesh:0.41.0", Vertx.vertx())
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

		// Now copy the jar in place
		deployPlugin();
		Thread.sleep(5000);
		
		//meshServer.client().
	}

	private void deployPlugin() throws IOException {
		copyJar();
		PluginResponse deploymentResponse = meshServer.client().deployPlugin(new PluginDeploymentRequest().setPath("comment-plugin.jar"))
			.blockingGet();
		System.out.println(deploymentResponse.toJson());
	}

	protected MeshRestClient meshClient() {
		return meshServer.client();
	}

	protected String serverURL() {
		return "http://" + meshServer.getHostname() + ":" + meshServer.getPort();
	}
}

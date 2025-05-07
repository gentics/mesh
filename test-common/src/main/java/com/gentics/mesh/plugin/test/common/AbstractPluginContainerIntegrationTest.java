package com.gentics.mesh.plugin.test.common;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.testcontainers.Testcontainers;

import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.plugin.test.common.util.ConfigSetupAction;
import com.gentics.mesh.plugin.test.common.util.MeshVersionUtil;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.docker.MeshContainer;
import com.gentics.mesh.test.docker.MeshDockerServer;

import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Abstract class for tests which are run against a mesh container. The tests deploys the final plugin jar in the container and runs the testcases.
 */
public abstract class AbstractPluginContainerIntegrationTest {

	public static final File PLUGINS_DIR = new File("target", "it_" + System.currentTimeMillis());

	static {
		PLUGINS_DIR.deleteOnExit();
		PLUGINS_DIR.mkdirs();
	}

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());

		/*
		The comment plugin requires this port to be exposed to the container.
		Other plugin tests should not be affected by this.
		This cannot be in a static block in the comment plugin because then it would be executed after the container
		was started which is too late.

		See also https://www.testcontainers.org/features/networking/#exposing-host-ports-to-the-container
		 */
		Testcontainers.exposeHostPorts(3025);
	}

	@ClassRule
	public static final MeshContainer meshServer = new File("license.key").exists() 
			? new MeshContainer(meshImage())
					.withFileSystemBind(Path.of("license.key").toAbsolutePath().toString(), "/mesh/license.key")
					.withFileSystemBind(PLUGINS_DIR.getAbsolutePath(), "/plugins")
					.waitForStartup()
			: new MeshContainer(meshImage())
					.withFileSystemBind(PLUGINS_DIR.getAbsolutePath(), "/plugins")
					.waitForStartup();

	public static void copyPlugin(String pluginId, ConfigSetupAction configFolderSetupAction) throws Exception {
		// 3. Copy pre-build plugin jar
		Path pluginJar = Files.list(Paths.get("target"))
			.filter(Files::isRegularFile)
			.filter(f -> !f.getFileName().toString().endsWith("sources.jar"))
			.filter(f -> !f.getFileName().toString().startsWith("original-"))
			.findFirst().orElse(null);
		if (pluginJar == null) {
			fail("Could not find plugin jar in target folder. Make sure you run mvn package before running integration tests");
		}
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, pluginId + "-plugin.jar"));

		// Prepare config
		File pluginDir = new File(PLUGINS_DIR, pluginId);
		pluginDir.deleteOnExit();
		pluginDir.mkdirs();
		configFolderSetupAction.setup(pluginDir);

		// Copy plugin jar
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, pluginId + "-plugin.jar"));

		// Deploy plugin
		PluginResponse response = meshServer.client().deployPlugin(new PluginDeploymentRequest().setPath(pluginId + "-plugin.jar")).blockingGet();
		System.out.println(response.toJson());
		PluginTestUtil.waitForPluginRegistration(meshServer, 5_000, pluginId);
	}

	public static String meshImage() {
		return MeshVersionUtil.getMatchingImage();
	}

	@AfterClass
	public static void cleanup() throws IOException {
		if (PLUGINS_DIR.exists()) {
			FileUtils.forceDelete(PLUGINS_DIR);
		}
	}
}

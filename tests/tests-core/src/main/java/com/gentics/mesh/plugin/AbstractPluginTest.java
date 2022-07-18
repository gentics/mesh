package com.gentics.mesh.plugin;

import static com.gentics.mesh.test.ClientHelper.call;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Dedicated abstract class which takes care of setting an independent plugin directory for each test.
 */
public class AbstractPluginTest extends AbstractMeshTest {

	public static final String PLUGINS_BASE = "/test-plugins/";

	public static final String BASIC_PATH = PLUGINS_BASE + "basic/target/basic-plugin-0.0.1-SNAPSHOT.jar";

	public static final String BASIC2_PATH = PLUGINS_BASE + "basic2/target/basic2-plugin-0.0.1-SNAPSHOT.jar";

	public static final String STATIC_PATH = PLUGINS_BASE + "static/target/static-plugin-0.0.1-SNAPSHOT.jar";

	public static final String STATIC2_PATH = PLUGINS_BASE + "static2/target/static2-plugin-0.0.1-SNAPSHOT.jar";

	public static final String CLIENT_PATH = PLUGINS_BASE + "client/target/client-plugin-0.0.1-SNAPSHOT.jar";

	public static final String FAILING_PATH = PLUGINS_BASE + "failing/target/failing-plugin-0.0.1-SNAPSHOT.jar";

	public static final String NON_MESH_PATH = PLUGINS_BASE + "non-mesh/target/non-mesh-plugin-0.0.1-SNAPSHOT.jar";

	public static final String CLASSLOADER_PATH = PLUGINS_BASE + "classloader/target/classloader-plugin-0.0.1-SNAPSHOT.jar";

	public static final String EXTENSION_PROVIDER_PATH = PLUGINS_BASE + "extension-provider/target/extension-provider-plugin-0.0.1-SNAPSHOT.jar";

	public static final String EXTENSION_CONSUMER_PATH = PLUGINS_BASE + "extension-consumer/target/extension-consumer-plugin-0.0.1-SNAPSHOT.jar";

	public static final String GRAPHQL_PATH = PLUGINS_BASE + "graphql/target/graphql-plugin-0.0.1-SNAPSHOT.jar";
	
	public static final String INVALID_GRAPHQL_PATH = PLUGINS_BASE + "invalid-graphql/target/invalid-graphql-plugin-0.0.1-SNAPSHOT.jar";

	@Before
	public void preparePluginDir() throws IOException {
		MeshPluginManager manager = pluginManager();
		manager.stop().blockingAwait(15, TimeUnit.SECONDS);
		// We need to init again since each test will setup a new plugin directory
		manager.start();
		cleanup();
	}

	@After
	public void cleanup() throws IOException {
		// undeploy all plugins
		grantAdmin();
		PluginListResponse plugins = call(() -> client().findPlugins());
		for (PluginResponse plugin : plugins.getData()) {
			call(() -> client().undeployPlugin(plugin.getId()));
		}

		File dir = new File(pluginDir());
		if (dir.exists()) {
			FileUtils.forceDelete(dir);
		}
		dir.mkdirs();
	}

	public void setPluginBaseDir(String baseDir) {
		File pluginDir = new File(baseDir);
		pluginDir.mkdirs();
		MeshOptions options = testContext.getOptions();
		options.setPluginDirectory(baseDir);
		pluginManager().start();
	}

	public void copy(String sourcePath, String name) throws IOException {
		copyFromResources(getClass(), sourcePath, pluginDir(), name);
	}

	public PluginResponse copyAndDeploy(String sourcePath, String name) throws IOException {
		copy(sourcePath, name);
		PluginDeploymentRequest request = new PluginDeploymentRequest().setPath(name);
		return call(() -> client().deployPlugin(request));
	}

	public void copyAndDeploy(String sourcePath, String name, HttpResponseStatus status, String key, String... params) throws IOException {
		copy(sourcePath, name);	
		PluginDeploymentRequest request = new PluginDeploymentRequest().setPath(name);
		call(() -> client().deployPlugin(request), status, key, params);
	}

	public static void copyFromResources(Class<?> cls, String sourcePath, String targetPath, String name) throws IOException {
		new File(targetPath).mkdirs();
		try (InputStream sourceRes = cls.getResourceAsStream(sourcePath)) {
			Files.copy(sourceRes, new File(targetPath, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}

package com.gentics.mesh.plugin;

import static com.gentics.mesh.test.ClientHelper.call;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.twelvemonkeys.io.FileUtil;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Dedicated abstract class which takes care of setting an independent plugin directory for each test.
 */
public class AbstractPluginTest extends AbstractMeshTest {

	public static final String BASIC_PATH = "target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar";

	public static final String BASIC2_PATH = "target/test-plugins/basic2/target/basic2-plugin-0.0.1-SNAPSHOT.jar";

	public static final String FAILING_PATH = "target/test-plugins/failing/target/failing-plugin-0.0.1-SNAPSHOT.jar";

	public static final String NON_MESH_PATH = "target/test-plugins/non-mesh/target/non-mesh-plugin-0.0.1-SNAPSHOT.jar";

	@Before
	public void preparePluginDir() throws IOException {
		try {
			MeshPluginManager manager = pluginManager();
			//pluginManager().init();
			manager.stop().blockingAwait(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		cleanup();
	}

	@After
	public void cleanup() throws IOException {
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
		pluginManager().init();
	}

	public PluginResponse copyAndDeploy(String sourcePath, String name) throws IOException {
		FileUtil.copy(new File(sourcePath), new File(pluginDir(), name));
		PluginDeploymentRequest request = new PluginDeploymentRequest().setPath(name);
		return call(() -> client().deployPlugin(request));
	}

	public void copyAndDeploy(String sourcePath, String name, HttpResponseStatus status, String key, String... params) throws IOException {
		new File(pluginDir()).mkdirs();
		FileUtil.copy(new File(sourcePath), new File(pluginDir(), name));
		PluginDeploymentRequest request = new PluginDeploymentRequest().setPath(name);
		call(() -> client().deployPlugin(request), status, key, params);

	}

}

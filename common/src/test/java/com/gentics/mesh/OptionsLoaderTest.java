package com.gentics.mesh;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.VertxOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

public class OptionsLoaderTest {

	@Test
	public void testOptionsLoader() {
		File confFile = new File(CONFIG_FOLDERNAME + "/" + MESH_CONF_FILENAME);
		if (confFile.exists()) {
			confFile.delete();
		}
		MeshOptions options = OptionsLoader.createOrloadOptions();
		assertNotNull(options);
		assertTrue("The file should have been created.", confFile.exists());
		assertNotNull("A keystore password should have been generated.", options.getAuthenticationOptions().getKeystorePassword());
		assertNotNull("The node name should have been generated.", options.getNodeName());
	}

	@Test
	public void testApplyEnvs() throws Exception {
		Map<String, String> envMap = new HashMap<>();
		envMap.put(MeshOptions.MESH_DEFAULT_LANG_ENV, "ru");
		envMap.put(MeshOptions.MESH_UPDATECHECK_ENV, "false");
		envMap.put(HttpServerConfig.MESH_HTTP_PORT_ENV, "8100");
		envMap.put(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "https://somewhere.com");
		envMap.put(MeshOptions.MESH_CLUSTER_INIT_ENV, "true");
		envMap.put(HttpServerConfig.MESH_HTTP_CORS_ORIGIN_PATTERN_ENV, "*");
		envMap.put(HttpServerConfig.MESH_HTTP_CORS_ENABLE_ENV, "true");
		envMap.put(VertxOptions.MESH_VERTX_EVENT_POOL_SIZE_ENV, "41");
		envMap.put(VertxOptions.MESH_VERTX_WORKER_POOL_SIZE_ENV, "42");
		envMap.put(MeshOptions.MESH_LOCK_PATH_ENV, "dummy/1234");
		envMap.put(MeshUploadOptions.MESH_BINARY_DIR_ENV, "/uploads");
		envMap.put(MonitoringConfig.MESH_MONITORING_HTTP_HOST_ENV, "0.0.0.0");
		envMap.put(ContentConfig.MESH_CONTENT_VERSIONING_ENV, "true");
		set(envMap);
		MeshOptions options = OptionsLoader.createOrloadOptions();
		assertEquals(8100, options.getHttpServerOptions().getPort());
		assertEquals("ru", options.getDefaultLanguage());
		assertFalse(options.isUpdateCheckEnabled());
		assertEquals("https://somewhere.com", options.getSearchOptions().getUrl());
		assertTrue(options.isInitClusterMode());
		assertTrue(options.getHttpServerOptions().getEnableCors());
		assertEquals(41, options.getVertxOptions().getEventPoolSize());
		assertEquals(42, options.getVertxOptions().getWorkerPoolSize());
		assertEquals("*", options.getHttpServerOptions().getCorsAllowedOriginPattern());
		assertEquals("dummy/1234", options.getLockPath());
		assertEquals("/uploads", options.getUploadOptions().getDirectory());
		assertEquals("0.0.0.0", options.getMonitoringOptions().getHost());
		assertTrue(options.getContentOptions().isVersioning());
	}

	@Test
	public void testApplyEnvsNull() throws Exception {
		Map<String, String> envMap = new HashMap<>();
		envMap.put(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");
		set(envMap);
		MeshOptions options = OptionsLoader.createOrloadOptions();
		assertNull(options.getSearchOptions().getUrl());
	}

	/**
	 * Override the environment variables.
	 * 
	 * @param newenv
	 * @throws Exception
	 */
	public static void set(Map<String, String> newenv) throws Exception {
		Class[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(env);
				Map<String, String> map = (Map<String, String>) obj;
				map.clear();
				map.putAll(newenv);
			}
		}
	}

	@Test
	public void testApplyArgs() {
		MeshOptions options = OptionsLoader.createOrloadOptions("-nodeName", "theNodeName", "-clusterName", "theClusterName");
		assertEquals("The node name should have been specified.", "theNodeName", options.getNodeName());
		assertEquals("The cluster name should have been specified.", "theClusterName", options.getClusterOptions().getClusterName());
		assertTrue("We specified the clusterName thus clustering should automatically be enabled.", options.getClusterOptions().isEnabled());
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidOptions() {
		MeshOptions options = new MeshOptions();
		options.getClusterOptions().setEnabled(true);
		options.validate();
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidOptions2() {
		MeshOptions options = new MeshOptions();
		options.getClusterOptions().setEnabled(true).setClusterName("someName");
		options.validate();
	}

	@Test
	public void testInvalidOptions3() {
		MeshOptions options = new MeshOptions();
		options.setNodeName("someNode");
		options.getClusterOptions().setEnabled(true).setClusterName("someName");
		options.validate();
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidOptions4() {
		MeshOptions options = new MeshOptions();
		options.getStorageOptions().setDirectory(null);
		options.getStorageOptions().setStartServer(true);
		options.validate();
	}

	@Test
	public void testInvalidOptions5() {
		MeshOptions options = new MeshOptions();
		options.getStorageOptions().setDirectory(null);
		options.validate();
	}

}

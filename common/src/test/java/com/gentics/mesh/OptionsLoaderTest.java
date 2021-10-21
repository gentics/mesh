package com.gentics.mesh;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.etc.config.DiskQuotaOptions;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.VertxOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.etc.config.search.MappingMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;

public class OptionsLoaderTest {

	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

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
		environmentVariables.set(MeshOptions.MESH_DEFAULT_LANG_ENV, "ru");
		environmentVariables.set(MeshOptions.MESH_UPDATECHECK_ENV, "false");
		environmentVariables.set(HttpServerConfig.MESH_HTTP_PORT_ENV, "8100");
		environmentVariables.set(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "https://somewhere.com");
		environmentVariables.set(MeshOptions.MESH_CLUSTER_INIT_ENV, "true");
		environmentVariables.set(HttpServerConfig.MESH_HTTP_CORS_ORIGIN_PATTERN_ENV, "*");
		environmentVariables.set(HttpServerConfig.MESH_HTTP_CORS_ENABLE_ENV, "true");
		environmentVariables.set(VertxOptions.MESH_VERTX_EVENT_POOL_SIZE_ENV, "41");
		environmentVariables.set(VertxOptions.MESH_VERTX_WORKER_POOL_SIZE_ENV, "42");
		environmentVariables.set(MeshOptions.MESH_LOCK_PATH_ENV, "dummy/1234");
		environmentVariables.set(MeshUploadOptions.MESH_BINARY_DIR_ENV, "/uploads");
		environmentVariables.set(MonitoringConfig.MESH_MONITORING_HTTP_HOST_ENV, "0.0.0.0");
		environmentVariables.set(ContentConfig.MESH_CONTENT_AUTO_PURGE_ENV, "true");
		environmentVariables.set(ElasticSearchOptions.MESH_ELASTICSEARCH_MAPPING_MODE_ENV, "STRICT");
		environmentVariables.set(ImageManipulatorOptions.MESH_IMAGE_CACHE_DIRECTORY_ENV, "data" + File.separator +"binaryImageCache");

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
		assertTrue(options.getContentOptions().isAutoPurge());
		assertEquals(MappingMode.STRICT, options.getSearchOptions().getMappingMode());
		assertEquals("data" + File.separator + "binaryImageCache", options.getImageOptions().getImageCacheDirectory());
	}

	@Test
	public void testApplyEnvsNull() throws Exception {
		environmentVariables.set(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");
		MeshOptions options = OptionsLoader.createOrloadOptions();
		assertNull(options.getSearchOptions().getUrl());
	}

	@Test
	public void testTrustedCertListOneItemEnv() {
		environmentVariables.set(HttpServerConfig.MESH_HTTP_SSL_TRUSTED_CERTS_ENV, "abc");
		MeshOptions options = OptionsLoader.createOrloadOptions();
		List<String> list = options.getHttpServerOptions().getTrustedCertPaths();
		assertEquals("The path list should contain oneentries", 1, list.size());
		assertEquals("abc", list.get(0));
	}

	@Test
	public void testTrustedCertListEnv() {
		environmentVariables.set(HttpServerConfig.MESH_HTTP_SSL_TRUSTED_CERTS_ENV, "abc,efg");
		MeshOptions options = OptionsLoader.createOrloadOptions();
		List<String> list = options.getHttpServerOptions().getTrustedCertPaths();
		assertEquals("The path list should contain two entries", 2, list.size());
		assertEquals("abc", list.get(0));
		assertEquals("efg", list.get(1));
	}

	@Test
	public void testEmptyTrustedCertListEnv() {
		environmentVariables.set(HttpServerConfig.MESH_HTTP_SSL_TRUSTED_CERTS_ENV, "");
		MeshOptions options = OptionsLoader.createOrloadOptions();
		List<String> list = options.getHttpServerOptions().getTrustedCertPaths();
		assertEquals("The path list should contain two entries", 0, list.size());
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
		options.getAuthenticationOptions().setKeystorePassword("ABC");
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
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		options.getStorageOptions().setDirectory(null);
		options.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDiskQuotaOptions1() {
		MeshOptions options = new MeshOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setReadOnlyThreshold("Not a number");
		options.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDiskQuotaOptions2() {
		MeshOptions options = new MeshOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setWarnThreshold("Not a number");
		options.validate();
	}

	@Test
	public void testLegalDiskQuotaOptions() {
		MeshOptions options = new MeshOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setReadOnlyThreshold("10M");
		diskQuotaOptions.setWarnThreshold("10%");

		// mock storage dir with 200M total space
		File storageDir = mock(File.class);
		when(storageDir.getTotalSpace()).thenReturn(200L * 1024 * 1024);

		// absolute read-only threshold must be 10M (configured)
		assertThat(diskQuotaOptions.getAbsoluteReadOnlyThreshold(storageDir)).as("Absolute read-only threshold").isEqualTo(10 * 1024 * 1024);
		// absolute warn threshold must be 20M (10% of 200M)
		assertThat(diskQuotaOptions.getAbsoluteWarnThreshold(storageDir)).as("Absolute warn threshold").isEqualTo(20 * 1024 * 1024);
		options.validate();
	}
}

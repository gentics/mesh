package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.MeshRestHttpClientImpl;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Completable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.ServiceHelper;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstract implementation for a Gentics Mesh plugin verticle.
 */
public abstract class AbstractPluginVerticle extends AbstractVerticle implements Plugin {

	private static String MANIFEST_FILENAME = "mesh-plugin.json";

	private static PluginManager manager = ServiceHelper.loadFactory(PluginManager.class);

	private static final Logger log = LoggerFactory.getLogger(AbstractPluginVerticle.class);

	private PluginManifest manifest;

	private MeshRestClient adminClient;

	public AbstractPluginVerticle() {
	}

	/**
	 * Return the preconfigured object mapper which is used to transform YAML documents.
	 * 
	 * @return
	 */
	public static ObjectMapper getYAMLMapper() {
		YAMLFactory factory = new YAMLFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}

	@Override
	public PluginManifest getManifest() {
		if (manifest == null) {
			try (InputStream in = getClass().getResourceAsStream("/" + MANIFEST_FILENAME)) {
				if (in == null) {
					throw new RuntimeException("Could find {" + MANIFEST_FILENAME + "} file in plugin classpath.");
				}
				String json = IOUtils.toString(in, Charset.defaultCharset());
				manifest = JsonUtil.readValue(json, PluginManifest.class);
				manifest.validate();
			} catch (IOException e) {
				throw new RuntimeException("Could not load {" + MANIFEST_FILENAME + "} file.");
			}
		}
		return manifest;
	}

	@Override
	public MeshRestClient adminClient() {
		String token = manager.adminToken();
		adminClient.setAPIKey(token);
		return adminClient;
	}

	@Override
	public File getStorageDir() {
		return new File(getPluginBaseDir(), "storage");
	}

	@Override
	public <T> T readConfig(Class<T> clazz) throws FileNotFoundException, IOException {
		File configFile = getConfigFile();
		if (!configFile.exists()) {
			return null;
		}
		try (FileInputStream fis = new FileInputStream(configFile)) {
			return getYAMLMapper().readValue(fis, clazz);
		}
	}

	@Override
	public <T> T writeConfig(T config) throws IOException {
		String yaml = getYAMLMapper().writeValueAsString(config);
		FileUtils.writeStringToFile(getConfigFile(), yaml, Charset.defaultCharset(), false);
		return config;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		log.info("Starting plugin {" + getName() + "}");
		createAdminClient();
		getPluginBaseDir().mkdirs();
		getStorageDir().mkdirs();

		manager.registerPlugin(this)
			.subscribe(startFuture::complete, startFuture::fail);
	}

	@Override
	public Completable initialize() {
		return Completable.complete();
	}

	@Override
	public String deploymentID() {
		// In Gentics Mesh we use shortened UUIDs instead of the full Uuids.
		return UUIDUtil.toShortUuid(super.deploymentID());
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		log.info("Stopping plugin {" + getName() + "}");
		if (adminClient != null) {
			adminClient.close();
		}

		manager.deregisterPlugin(this)
			.subscribe(stopFuture::complete, stopFuture::fail);
	}

	@Override
	public Completable prepareStop() {
		return Completable.complete();
	}

	/**
	 * Return a wrapped routing context.
	 * 
	 * @param rc
	 * @return
	 */
	public PluginContext wrap(RoutingContext rc) {
		return new PluginContext(rc);
	}

	/**
	 * Return a wrapped routing context handler
	 * 
	 * @param rc
	 * @return
	 */
	public Handler<RoutingContext> wrapHandler(Handler<PluginContext> handler) {
		return rc -> handler.handle(wrap(rc));
	}

	private void createAdminClient() {
		MeshOptions options = Mesh.mesh().getOptions();
		int port = options.getHttpServerOptions().getPort();
		String host = options.getHttpServerOptions().getHost();
		adminClient = new MeshRestHttpClientImpl(host, port, false, vertx);
	}

	/**
	 * Return the plugin base directory in which the config and the storage folder resides.
	 * 
	 * @return
	 */
	protected File getPluginBaseDir() {
		MeshOptions options = Mesh.mesh().getOptions();
		String pluginDir = options.getPluginDirectory();
		String apiName = getManifest().getApiName();
		return new File(pluginDir, apiName);
	}

	/**
	 * Return the plugin configuration file.
	 * 
	 * @return
	 */
	protected File getConfigFile() {
		return new File(getPluginBaseDir(), "config.yml");
	}

}
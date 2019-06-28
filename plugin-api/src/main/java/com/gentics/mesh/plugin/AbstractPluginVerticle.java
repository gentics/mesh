package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
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
	 * Use {@link PluginConfigUtil#getYAMLMapper()} instead.
	 * @deprecated
	 * @return
	 */
	@Deprecated
	public static ObjectMapper getYAMLMapper() {
		return PluginConfigUtil.getYAMLMapper();
	}

	@Override
	public PluginManifest getManifest() {
		if (manifest == null) {
			try (InputStream in = getClass().getResourceAsStream("/" + MANIFEST_FILENAME)) {
				if (in == null) {
					throw new RuntimeException("Could find {" + MANIFEST_FILENAME + "} file in plugin classpath.");
				}
				String json = IOUtils.toString(in, StandardCharsets.UTF_8);
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
		if (!configFile.canRead()) {
			return null;
		}
		T config = PluginConfigUtil.loadConfig(configFile, null, clazz);

		// try to load local config file
		File localConfigFile = getLocalConfigFile();
		if (localConfigFile.canRead()) {
			config = PluginConfigUtil.loadConfig(localConfigFile, config, clazz);
		}
		return config;
	}

	@Override
	public <T> T writeConfig(T config) throws IOException {
		PluginConfigUtil.writeConfig(getConfigFile(), config);		
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
	 *            Vert.x routing context
	 * @return Wrapped context
	 */
	public PluginContext wrap(RoutingContext rc) {
		return new PluginContext(rc);
	}

	/**
	 * Return a wrapped routing context handler
	 * 
	 * @param handler
	 *            Handler to be wrapped
	 * @return Wrapped handler
	 */
	public Handler<RoutingContext> wrapHandler(Handler<PluginContext> handler) {
		return rc -> handler.handle(wrap(rc));
	}

	private void createAdminClient() {
		MeshOptions options = Mesh.mesh().getOptions();
		int port = options.getHttpServerOptions().getPort();
		String host = options.getHttpServerOptions().getHost();
		adminClient = MeshRestClient.create(host, port, false);
	}

	/**
	 * Return the plugin base directory in which the config and the storage folder resides.
	 * 
	 * @return Plugin base dir
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
	 * @return Plugin configuration file
	 */
	protected File getConfigFile() {
		return new File(getPluginBaseDir(), "config.yml");
	}

	/**
	 * Return the local overriding plugin configuration file.
	 * 
	 * @return
	 */
	protected File getLocalConfigFile() {
		return new File(getPluginBaseDir(), "config.local.yml");
	}
}

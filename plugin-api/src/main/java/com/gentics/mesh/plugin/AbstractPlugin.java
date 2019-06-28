package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for a Gentics Mesh plugin verticle.
 */
public abstract class AbstractPlugin extends Plugin implements MeshPlugin {

	private static String MANIFEST_FILENAME = "mesh-plugin.json";

	private static final Logger log = LoggerFactory.getLogger(AbstractPlugin.class);

	private static final String WILDCARD_IP = "0.0.0.0";

	private static final String LOOPBACK_IP = "127.0.0.1";

	private PluginManifest manifest;

	private static PluginWrapper wrapper;

	public AbstractPlugin(PluginWrapper wrapper) {
		super(wrapper);
		AbstractPlugin.wrapper = wrapper;
	}

	/**
	 * Use {@link PluginConfigUtil#getYAMLMapper()} instead.
	 * 
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
	public void start() {
		log.info("Starting plugin {" + getName() + "}");
		createAdminClient();
		getPluginBaseDir().mkdirs();
		getStorageDir().mkdirs();

		manager.registerPlugin(this).blockingAwait();
	}

	@Override
	public Completable initialize() {
		return Completable.complete();
	}

	@Override
	public String deploymentID() {
		return getWrapper().getPluginId();
	}

	@Override
	public void stop() {
		log.info("Stopping plugin {" + getName() + "}");
		if (adminClient != null) {
			adminClient.close();
		}

		manager.deregisterPlugin(this).blockingAwait();
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

	private String determineHostString(MeshOptions options) {
		String host = options.getHttpServerOptions().getHost();
		return WILDCARD_IP.equals(host) ? LOOPBACK_IP : host;
	}

	private void createAdminClient() {
		MeshOptions options = Mesh.mesh().getOptions();
		int port = options.getHttpServerOptions().getPort();
		String host = this.determineHostString(options);
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

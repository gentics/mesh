package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.pf4j.Plugin;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstract implementation for a Gentics Mesh plugin verticle.
 */
public abstract class AbstractPlugin extends Plugin implements MeshPlugin {

	private static final Logger log = LoggerFactory.getLogger(AbstractPlugin.class);

	private MeshRestClient adminClient;

	private final PluginEnvironment env;

	public AbstractPlugin() {
		super(null);
		this.env = null;
	}

	public AbstractPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper);
		this.env = env;
	}

	@Override
	public String id() {
		return getWrapper().getPluginId();
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
		PluginDescriptor descriptor = getWrapper().getDescriptor();
		if (descriptor instanceof MeshPluginDescriptor) {
			return ((MeshPluginDescriptor) descriptor).toPluginManifest();
		}
		throw new RuntimeException(
			"The found plugin descriptor does not contain Gentics Mesh information. Found {" + descriptor + "}");
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
	public final void start() {
		log.info("Starting plugin {" + name() + "}");
		createAdminClient();
		getPluginBaseDir().mkdirs();
		getStorageDir().mkdirs();
	}

	@Override
	public Completable initialize() {
		return Completable.complete();
	}

	@Override
	public final void stop() {
		log.info("Stopping plugin {" + name() + "}");
		if (adminClient != null) {
			adminClient.close();
		}
	}

	@Override
	public Completable shutdown() {
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
	 * Return the plugin base directory in which the config and the storage folder resides.
	 * 
	 * @return Plugin base dir
	 */
	protected File getPluginBaseDir() {
		MeshOptions options = Mesh.mesh().getOptions();
		String pluginDir = options.getPluginDirectory();
		String id = id();
		return new File(pluginDir, id);
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

	@Override
	public MeshRestClient adminClient() {
		return adminClient;
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

	protected void createAdminClient() {
		adminClient = env.createAdminClient();
	}

	@Override
	public Vertx vertx() {
		return env.vertx();
	}

	/**
	 * Return the plugin environment
	 * 
	 * @return
	 */
	public PluginEnvironment environment() {
		return env;
	}

}

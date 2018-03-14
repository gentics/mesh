package com.gentics.mesh.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.manager.api.PluginManager;

import ch.qos.logback.core.Context;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginManagerImpl implements PluginManager {

	private static final String PLUGIN_MANIFEST_FILENAME = "mesh-plugin.json";

	private static final String PLUGIN_JAR_FILENAME = "plugin.jar";

	private static final Logger log = LoggerFactory.getLogger(PluginManagerImpl.class);

	private String pluginFolder;

	private Framework framework;

	@Inject
	public PluginManagerImpl() {
	}

	public void init(MeshOptions options) {
		try {
			this.framework = startFramework();
		} catch (BundleException e) {
			log.error("Error while starting OSGi framework", e);
		}

		BundleContext context = framework.getBundleContext();

		List<Bundle> installedBundles = new LinkedList<Bundle>();

		try {
			// installedBundles.add(context.installBundle("file:org.apache.felix.shell-1.4.3.jar"));
			// installedBundles.add(context.installBundle("file:org.apache.felix.shell.tui-1.4.1.jar"));

			installedBundles.add(context.installBundle("file:mesh-hello-world-plugin-0.18.0-SNAPSHOT.jar"));
			for (Bundle bundle : installedBundles) {
				bundle.start();
				ServiceReference<Plugin> ref = bundle.getBundleContext().getServiceReference(Plugin.class);
				Plugin plugin = bundle.getBundleContext().getService(ref);
				System.out.println("pppppppppp " + plugin.getName());
			}

		} catch (BundleException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// this.pluginFolder = options.getPluginDirectory();
		// try {
		// // Search for installed plugins
		// Stream<File> zipFiles = Files.list(Paths.get(pluginFolder)).filter(Files::isRegularFile).filter((f) -> {
		// return f.getFileName().toString().endsWith(".zip");
		// }).map(p -> p.toFile());
		// zipFiles.forEach(file -> {
		// registerPlugin(file);
		// });
		// } catch (IOException e) {
		// log.error("Error while reading plugins from plugin folder {" + pluginFolder + "}", e);
		// }
	}

	private Framework startFramework() throws BundleException {
		FrameworkFactory frameworkFactory = ServiceLoader.load(
			FrameworkFactory.class).iterator().next();
		Map<String, String> config = new HashMap<String, String>();
		StringBuilder frameworkPackages = new StringBuilder();
		frameworkPackages.append("com.gentics.mesh.plugin.rest,");
		frameworkPackages.append("io.vertx.core;version=3.5.1,");
		frameworkPackages.append("io.vertx.core.http;version=3.5.1,");
		frameworkPackages.append("io.vertx.ext.web;version=3.5.1,");
		frameworkPackages.append("com.fasterxml.jackson.annotation;version=2.9.0");
		config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, frameworkPackages.toString());
		Framework framework = frameworkFactory.newFramework(config);
		framework.start();
		BundleContext context = framework.getBundleContext();
		context.addBundleListener(b -> {
			System.out.println("Bundle: " + b.getType());
		});
		context.addServiceListener(event -> {
			System.out.println("Event" + event.getType());
			System.out.println("Type: " + event.getSource().getClass().getName());
			// Plugin manager = (PluginManager) context.getService(event.getServiceReference());
			// manager.registerPlugin(this);
		});
		return framework;
	}

	@Override
	public void registerPlugin(Plugin plugin) {
		System.out.println("REGISTER Plugin " + plugin.getName());
	}

	private void registerPlugin(File file) {
		// Check for plugin collisions

		log.info("Registering plugin {" + file + "}");
		try (ZipFile zipFile = new ZipFile(file)) {

			// Load the manifest
			ZipEntry manifestEntry = zipFile.getEntry(PLUGIN_MANIFEST_FILENAME);
			if (manifestEntry == null) {
				throw new PluginException("The plugin manifest file {" + PLUGIN_MANIFEST_FILENAME + "} could not be found in the plugin archive.");
			}
			try (InputStream ins = zipFile.getInputStream(manifestEntry)) {
				String json = IOUtils.toString(ins);
				PluginManifest manifest = JsonUtil.readValue(json, PluginManifest.class);
				manifest.validate();
			}

			// Load the jar
			ZipEntry jarEntry = zipFile.getEntry(PLUGIN_JAR_FILENAME);
			if (jarEntry == null) {
				throw new PluginException("The plugin jar file {" + PLUGIN_JAR_FILENAME + "} could not be found in the plugin archive.");
			}
			try (InputStream ins = zipFile.getInputStream(jarEntry)) {
				try {

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Extract the static resources
		} catch (Exception e) {
			log.error("Error while loading plugin from file {" + file + "}", e);
		}

	}

}

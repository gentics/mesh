package com.gentics.mesh.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginManager {

	private static final String PLUGIN_MANIFEST_FILENAME = "mesh-plugin.json";

	private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

	private String pluginFolder;

	@Inject
	public PluginManager() {
	}

	public void init(MeshOptions options) {
		this.pluginFolder = options.getPluginDirectory();
		try {
			// Search for installed plugins
			Stream<File> zipFiles = Files.list(Paths.get(pluginFolder)).filter(Files::isRegularFile).filter((f) -> {
				return f.getFileName().toString().endsWith(".zip");
			}).map(p -> p.toFile());
			zipFiles.forEach(file -> {
				registerPlugin(file);
			});
		} catch (IOException e) {
			log.error("Error while reading plugins from plugin folder {" + pluginFolder + "}", e);
		}
	}

	private void registerPlugin(File file) {
		log.info("Registering plugin {" + file + "}");
		try (ZipFile zipFile = new ZipFile(file)) {

			// Load the manifest
			ZipEntry entry = zipFile.getEntry(PLUGIN_MANIFEST_FILENAME);
			if (entry == null) {
				throw new PluginException("The plugin manifest file {" + PLUGIN_MANIFEST_FILENAME + "} could not be found in the plugin archive.");
			}
			try (InputStream ins = zipFile.getInputStream(entry)) {
				String json = IOUtils.toString(ins);
				PluginManifest manifest = JsonUtil.readValue(json, PluginManifest.class);
				manifest.validate();
			}

			// Load the jar

			// Extract the static resources
		} catch (Exception e) {
			log.error("Error while loading plugin from file {" + file + "}", e);
		}

	}

}

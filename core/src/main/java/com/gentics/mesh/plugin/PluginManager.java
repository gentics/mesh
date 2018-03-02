package com.gentics.mesh.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginManager {

	private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

	private String pluginFolder;

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
	}

}

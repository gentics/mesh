package com.gentics.mesh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that is used to create and load mesh options.
 */
public final class OptionsLoader {

	private static final Logger log = LoggerFactory.getLogger(OptionsLoader.class);

	public static final String MESH_CONF_FILENAME = "mesh.yml";

	private OptionsLoader() {

	}

	/**
	 * Load the main mesh configuration file.
	 */
	public static MeshOptions createOrloadOptions() {
		File confFile = new File(MESH_CONF_FILENAME);
		InputStream ins = Mesh.class.getResourceAsStream("/" + MESH_CONF_FILENAME);
		// 1. Try to load from classpath
		if (ins != null) {
			log.info("Loading configuration file from classpath.");
			MeshOptions configuration = loadConfiguration(ins);
			if (configuration != null) {
				return configuration;
			}
		} else {
			log.info("Configuration file {" + MESH_CONF_FILENAME + "} was not found within classpath.");
		}
		// 2. Try to use config file
		if (confFile.exists()) {
			try {
				log.info("Loading configuration file {" + confFile + "}.");
				MeshOptions configuration = loadConfiguration(new FileInputStream(confFile));
				if (configuration != null) {
					return configuration;
				}
			} catch (FileNotFoundException e) {
				log.error("Could not load configuration file {" + confFile.getAbsolutePath() + "}.", e);
			}
		} else {
			log.info("Configuration file {" + MESH_CONF_FILENAME + "} was not found within filesystem.");

			YAMLFactory factory = new YAMLFactory();
			ObjectMapper mapper = new ObjectMapper(factory);
			mapper.setSerializationInclusion(Include.NON_NULL);

			try {
				MeshOptions conf = new MeshOptions();
				System.out.println(mapper.writeValueAsString(conf));
				System.exit(1);
				//				FileUtils.writeStringToFile(confFile, yaml.dump(conf));
				//				log.info("Saved default configuration to file {" + confFile.getAbsolutePath() + "}.");
				//				return conf;
			} catch (IOException e) {
				log.error("Error while saving default configuration to file {" + confFile.getAbsolutePath() + "}.", e);
			}
		}
		// 2. No luck - use default config
		log.info("Loading default configuration.");
		return new MeshOptions();
	}

	/**
	 * Load the configuration from the stream.
	 * 
	 * @param ins
	 * @return
	 */
	private static MeshOptions loadConfiguration(InputStream ins) {
		if (ins == null) {
			log.info("Config file {" + MESH_CONF_FILENAME + "} not found. Using default configuration.");
			return new MeshOptions();
		}

		Yaml yaml = new Yaml();
		try {
			return yaml.loadAs(ins, MeshOptions.class);
		} catch (Exception e) {
			log.error("Could not parse configuration.", e);
			throw new RuntimeException("Could not parse options file", e);
		}
	}

}

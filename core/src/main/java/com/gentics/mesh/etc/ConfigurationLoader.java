package com.gentics.mesh.etc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.etc.config.MeshConfiguration;

public final class ConfigurationLoader {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

	public static final String MESH_CONF_FILENAME = "mesh.json";

	private ConfigurationLoader() {

	}

	/**
	 * Load the main mesh configuration file.
	 * 
	 * @return
	 */
	public static MeshConfiguration createOrloadConfiguration() {
		File confFile = new File(MESH_CONF_FILENAME);
		InputStream ins = Mesh.class.getResourceAsStream("/" + MESH_CONF_FILENAME);
		// 1. Try to load from classpath
		if (ins != null) {
			log.info("Loading configuration file from classpath.");
			MeshConfiguration configuration = loadConfiguration(ins);
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
				MeshConfiguration configuration = loadConfiguration(new FileInputStream(confFile));
				if (configuration != null) {
					return configuration;
				}
			} catch (FileNotFoundException e) {
				log.error("Could not load configuration file {" + confFile.getAbsolutePath() + "}.", e);
			}
		} else {
			log.info("Configuration file {" + MESH_CONF_FILENAME + "} was not found within filesystem.");
			ObjectMapper mapper = new ObjectMapper();
			try {
				MeshConfiguration conf = new MeshConfiguration();
				FileUtils.writeStringToFile(confFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conf));
				log.info("Saved default configuration to file {" + confFile.getAbsolutePath() + "}.");
				return conf;
			} catch (IOException e) {
				log.error("Error while saving default configuration to file {" + confFile.getAbsolutePath() + "}.", e);
			}
		}
		// 2. No luck - use default config
		log.info("Loading default configuration.");
		return new MeshConfiguration();
	}

	/**
	 * Load the configuration from the inputstream
	 * 
	 * @param ins
	 * @return
	 */
	private static MeshConfiguration loadConfiguration(InputStream ins) {
		// TODO use java 8 optionals
		if (ins == null) {
			log.info("Config file {" + MESH_CONF_FILENAME + "} not found. Using default configuration.");
			return new MeshConfiguration();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(ins, MeshConfiguration.class);
		} catch (IOException e) {
			log.error("Could not parse configuration.", e);
		}
		System.exit(1);
		return null;
	}

}

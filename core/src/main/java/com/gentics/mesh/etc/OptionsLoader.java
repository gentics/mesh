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
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Utility class that is used to create and load mesh options.
 */
public final class OptionsLoader {

	private static final Logger log = LoggerFactory.getLogger(OptionsLoader.class);

	public static final String MESH_CONF_FILENAME = "mesh.json";

	private OptionsLoader() {

	}

	/**
	 * Load the main mesh configuration file.
	 */
	public static MeshOptions createOrloadOptions() {
		File confFile = new File(MESH_CONF_FILENAME);
		InputStream ins = MeshImpl.class.getResourceAsStream("/" + MESH_CONF_FILENAME);
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
			ObjectMapper mapper = new ObjectMapper();
			try {
				MeshOptions conf = new MeshOptions();
				FileUtils.writeStringToFile(confFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conf));
				log.info("Saved default configuration to file {" + confFile.getAbsolutePath() + "}.");
				return conf;
			} catch (IOException e) {
				log.error("Error while saving default configuration to file {" + confFile.getAbsolutePath() + "}.", e);
			}
		}
		// 2. No luck - use default config
		log.info("Loading default configuration.");
		return new MeshOptions();
	}

	/**
	 * Load the configuration from the inputstream
	 * 
	 * @param ins
	 * @return
	 */
	private static MeshOptions loadConfiguration(InputStream ins) {
		// TODO use java 8 optionals
		if (ins == null) {
			log.info("Config file {" + MESH_CONF_FILENAME + "} not found. Using default configuration.");
			return new MeshOptions();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(ins, MeshOptions.class);
		} catch (IOException e) {
			log.error("Could not parse configuration.", e);
			throw new RuntimeException("Could not parse options file", e);
		}
	}

}

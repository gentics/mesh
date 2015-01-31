package com.gentics.cailun.etc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.cli.CaiLun;

public final class ConfigurationLoader {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

	public static final String CAILUN_CONF_FILENAME = "cailun.json";

	private ConfigurationLoader() {

	}

	/**
	 * Load the main cailun configuration file.
	 * 
	 * @return
	 */
	public static CaiLunConfiguration loadConfiguration() {
		File confFile = new File(CAILUN_CONF_FILENAME);
		InputStream ins = CaiLun.class.getResourceAsStream("/" + CAILUN_CONF_FILENAME);
		// 1. Try to load from classpath
		if (ins != null) {
			log.info("Loading configuration file from classpath.");
			CaiLunConfiguration configuration = loadConfiguration(ins);
			if (configuration != null) {
				return configuration;
			}
		} else {
			log.info("Configuration file {" + CAILUN_CONF_FILENAME + "} was not found within classpath.");
		}
		// 2. Try to use config file
		if (confFile.exists()) {
			try {
				log.info("Loading configuration file {" + confFile + "}.");
				CaiLunConfiguration configuration = loadConfiguration(new FileInputStream(confFile));
				if (configuration != null) {
					return configuration;
				}
			} catch (FileNotFoundException e) {
				log.error("Could not load configuration file {" + confFile + "}.", e);
			}
		} else {
			log.info("Configuration file {" + CAILUN_CONF_FILENAME + "} was not found within filesystem.");
		}
		// 2. No luck - use default config
		log.info("Loading default configuration.");
		return new CaiLunConfiguration();
	}

	/**
	 * Load the configuration from the inputstream
	 * 
	 * @param ins
	 * @return
	 */
	private static CaiLunConfiguration loadConfiguration(InputStream ins) {
		// TODO use java 8 optionals
		if (ins == null) {
			log.info("Config file {" + CAILUN_CONF_FILENAME + "} not found. Using default configuration.");
			return new CaiLunConfiguration();
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(ins, CaiLunConfiguration.class);
		} catch (IOException e) {
			log.error("Could not parse configuration.", e);
		}
		System.exit(1);
		return null;
	}

}

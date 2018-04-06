package com.gentics.mesh.context.impl;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class LoggingConfigurator {

	private static final String DEFAULT_LOGBACK_FILE_NAME = "logback.default.xml";

	/**
	 * Initialize the logback logger and ensure that the logback file is placed in the config folder
	 */
	public static void init() {
		File logbackFile = new File("config", "logback.xml");
		if (!logbackFile.exists()) {
			writeFile(logbackFile);
		}
		System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		LoggerFactory.getLogger(LoggingConfigurator.class);
	}

	/**
	 * Write the default logback file to the config folder.
	 * 
	 * @param file
	 */
	public static void writeFile(File file) {
		try {
			String resourcePath = "/" + DEFAULT_LOGBACK_FILE_NAME;
			InputStream configIns = LoggingConfigurator.class.getResourceAsStream(resourcePath);
			if (configIns == null) {
				throw new Exception("Could not find default logback file {" + resourcePath + "} within classpath.");
			}
			StringWriter writer = new StringWriter();
			IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
			String configString = writer.toString();
			FileUtils.writeStringToFile(file, configString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

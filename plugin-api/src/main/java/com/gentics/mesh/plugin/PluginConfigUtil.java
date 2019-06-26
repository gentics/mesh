package com.gentics.mesh.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PluginConfigUtil {
	
	private static final Logger log = LoggerFactory.getLogger(PluginConfigUtil.class);
	
	/**
	 * Load YAML config from input Stream
	 * If a config Class is provided, new Config is merged to current config
	 * 
	 * @param f 
	 * @param config
	 * @param clazzOfT
	 * @return
	 * @throws IOException
	 */
	public static <T> T loadConfig(File f, T config, Class<T> clazzOfT) throws IOException {
		ObjectMapper mapper = getYAMLMapper();

		try (FileInputStream fis = new FileInputStream(f)) {
			if (config == null) {
				return mapper.readValue(fis, clazzOfT);
			} else {
				JsonObject baseConfigJson = new JsonObject(Json.encode(config));
				Object newConfig = mapper.readValue(fis, Object.class);
				JsonObject newConfigJson = new JsonObject(new ObjectMapper().writeValueAsString(newConfig));
				JsonObject mergedJson = baseConfigJson.mergeIn(newConfigJson, true);

				if (log.isTraceEnabled()) {
					log.trace("Base config:\n" + baseConfigJson.encodePrettily());
					log.trace("New config:\n" + newConfigJson.encodePrettily());
					log.trace("Merged config:\n" + mergedJson.encodePrettily());
				}

				return mapper.readValue(mergedJson.encode(), clazzOfT);
			}
		}
	}
	
	/**
	 * Write config Class to YAML config file
	 * 
	 * @param file
	 * @param config
	 * @throws IOException
	 */
	public static <T> void writeConfig(File file, T config) throws IOException {
		String yaml = getYAMLMapper().writeValueAsString(config);
		FileUtils.writeStringToFile(file, yaml, StandardCharsets.UTF_8, false);
	}
	
	/**
	 * Return the preconfigured object mapper which is used to transform YAML documents.
	 * 
	 * @return
	 */
	public static ObjectMapper getYAMLMapper() {
		YAMLFactory factory = new YAMLFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}
	

}

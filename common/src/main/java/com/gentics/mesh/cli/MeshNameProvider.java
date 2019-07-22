package com.gentics.mesh.cli;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshNameProvider {

	private static final Logger log = LoggerFactory.getLogger(MeshNameProvider.class);

	private JSONObject names;
	private JSONObject adjectives;

	public static final String NAME_JSON_FILENAME = "pokemon.json";
	public static final String ADJECTIVES_JSON_FILENAME = "adjectives.json";

	private static MeshNameProvider instance;

	public MeshNameProvider() throws Exception {
		init();
	}

	public static MeshNameProvider getInstance() {
		if (instance == null) {
			try {
				instance = new MeshNameProvider();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	public void init() throws Exception {
		if (names == null) {
			names = loadJson(NAME_JSON_FILENAME);
		}

		if (adjectives == null) {
			adjectives = loadJson(ADJECTIVES_JSON_FILENAME);
		}
	}

	private JSONObject loadJson(String filename) throws Exception {

		final InputStream ins = MeshNameProvider.class.getResourceAsStream("/json/" + filename);
		if (ins == null) {
			log.error("Json could not be loaded from classpath file {" + filename + "}");
			throw new FileNotFoundException("Could not find json file {" + filename + "}");
		} else {
			StringWriter writer = new StringWriter();
			try {
				IOUtils.copy(ins, writer, StandardCharsets.UTF_8);
				JSONObject object = new JSONObject(writer.toString());
				return object;
			} catch (Exception e) {
				log.error("Error while parsing json file {" + filename + "}", e);
				throw e;
			}
		}
	}

	/**
	 * Generate a random name.
	 * 
	 * @return
	 */
	public String getRandomName() {
		try {
			JSONArray adjArray = adjectives.getJSONArray("data");
			int randomAdjectiveIndex = (int) (Math.random() * adjArray.length());
			String partA = StringUtils.trim(adjArray.getString(randomAdjectiveIndex));
			LocalDate now = getDate();
			if (now.getDayOfMonth() == 1 && now.getMonth() == Month.APRIL) {
				return partA + " Skynet";
			}
			JSONArray nameArray = names.getJSONArray("data");

			int randomNameIndex = (int) (Math.random() * nameArray.length());

			String partB = StringUtils.trim(nameArray.getString(randomNameIndex));
			return partA + " " + partB;
		} catch (Exception e) {
			log.error("Error while getting random name.", e);
			return "Unknown";
		}
	}

	public LocalDate getDate() {
		return LocalDate.now();
	}
}

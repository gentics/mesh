package com.gentics.mesh.cli;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshNameProvider {

	private static final Logger log = LoggerFactory.getLogger(MeshNameProvider.class);

	private static JSONObject names;

	public static final String NAME_JSON_FILENAME = "pokemon.json";

	private static String name;

	public static String getName() {
		if (name == null) {
			synchronized (MeshNameProvider.class) {
				if (name == null) {
					name = getRandomName();
				}
			}
		}
		return name;
	}

	public static void reset() {
		String oldName = name;
		String newName = oldName;
		while (oldName.equals(newName)) {
			newName = getRandomName();
		}
		name = newName;

	}

	protected static String getRandomName() {

		if (names == null) {
			final InputStream ins = MeshNameProvider.class.getResourceAsStream("/" + NAME_JSON_FILENAME);
			if (ins == null) {
				log.error("Names could not be loaded from classpath file {" + NAME_JSON_FILENAME + "}");
			} else {
				StringWriter writer = new StringWriter();
				try {
					IOUtils.copy(ins, writer);
					names = new JSONObject(writer.toString());

				} catch (Exception e) {
					log.error("Error while loading name json file {" + NAME_JSON_FILENAME + "}", e);
				}
			}
		}

		try {
			JSONArray array = names.getJSONArray("data");
			int random = (int) (Math.random() * array.length());
			return array.getJSONObject(random).getString("name");
		} catch (Exception e) {
			log.error("Error while getting random name.", e);
			return "Unknown";
		}
	}
}

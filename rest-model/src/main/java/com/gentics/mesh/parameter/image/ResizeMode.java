package com.gentics.mesh.parameter.image;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Enum which represents the implemented resize modes.
 */
public enum ResizeMode {
	FORCE("force", 
			"The force mode will resize the image to the specified dimensions. This can lead to a distorted image when the aspect ratio of the source image does not match the destination aspect ratio"),

	SMART("smart",
			"The smart mode will resize the image proportionally so that the resulting destination format matches the source format in at least one dimension. No distortion of the image will occur.");

	private final String key;
	private final String description;

	private static final Map<String, ResizeMode> lookup = new HashMap<>();

	static {
		// Create reverse lookup hash map
		for (ResizeMode m : ResizeMode.values()) {
			lookup.put(m.getKey(), m);
		}
	}

	private ResizeMode(String key, String description) {
		this.key = key;
		this.description = description;
	}

	/**
	 * Returns the mode key.
	 * 
	 * @return
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Returns the mode's description.
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the resize mode for the specified key.
	 * 
	 * @param key
	 * @return Found mode or null if the key matches no mode.
	 */
	public static ResizeMode get(String key) {
		return lookup.get(key);
	}

	/**
	 * Return the description for all resize modes.
	 * 
	 * @return
	 */
	public static String description() {
		StringBuilder builder = new StringBuilder();
		for (ResizeMode m : ResizeMode.values()) {
			builder.append("\n");
			builder.append(m.key + " : " + m.getDescription());
		}
		return builder.toString();
	}
}

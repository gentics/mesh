package com.gentics.mesh.parameter.image;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Enum which represents the implemented crop modes.
 */
public enum CropMode {

	RECT("rect", "The rect mode will work in combination with the " + ImageManipulationParameters.RECT_QUERY_PARAM_KEY
			+ " parameter and crop the specified area."),

	FOCALPOINT("fp",
			"The fp mode will utilize the specified or pre-selected focal point and crop the image according to the position of the focus point and the specified image size.");

	private final String key;
	private final String description;

	private static final Map<String, CropMode> lookup = new HashMap<>();

	static {
		// Create reverse lookup hash map
		for (CropMode m : CropMode.values()) {
			lookup.put(m.getKey(), m);
		}
	}

	private CropMode(String key, String description) {
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
	 * Get the crop mode for the specified key.
	 * 
	 * @param key
	 * @return Found mode or null if the key matches no mode.
	 */
	public static CropMode get(String key) {
		return lookup.get(key);
	}

	/**
	 * Return the description for all crop modes.
	 * 
	 * @return
	 */
	public static String description() {
		StringBuilder builder = new StringBuilder();
		for (CropMode m : CropMode.values()) {
			builder.append("\n");
			builder.append(m.key + " : " + m.getDescription());
		}
		return builder.toString();
	}

}

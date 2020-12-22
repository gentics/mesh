package com.gentics.mesh;

/**
 * Public configuration keys and values for various parts. 
 */
public final class MeshEnv {

	/**
	 * Default name of the configuration folder
	 */
	public static String CONFIG_FOLDERNAME = "config";

	/**
	 * Static loader which overrides the default config folder name when the class gets loaded.
	 */
	static {
		String customConfDir = System.getProperty("mesh.confDirName");
		if (customConfDir != null) {
			CONFIG_FOLDERNAME = customConfDir;
		}
	}

	/**
	 * Name of the Gentics Mesh config file.
	 */
	public static final String MESH_CONF_FILENAME = "mesh.yml";
}

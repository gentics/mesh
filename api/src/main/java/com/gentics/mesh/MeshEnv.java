package com.gentics.mesh;

public final class MeshEnv {

	public static String CONFIG_FOLDERNAME = "config";

	static {
		String customConfDir = System.getProperty("mesh.confDirName");
		if (customConfDir != null) {
			CONFIG_FOLDERNAME = customConfDir;
		}
	}

	public static final String MESH_CONF_FILENAME = "mesh.yml";
}

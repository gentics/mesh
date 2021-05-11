package com.gentics.mesh.plugin;

/**
 * The following class is present in the plugin and in the mesh test classes.
 */
public class ConflictingClass {

	public static String scope = "mesh";

	/**
	 * This method has a different signature in the plugin.
	 * 
	 * @param arg
	 * @return
	 */
	public static String check(String arg) {
		return "mesh";
	}
}

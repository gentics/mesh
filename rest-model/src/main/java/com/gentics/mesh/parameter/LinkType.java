package com.gentics.mesh.parameter;

/**
 * Link Replacing type
 */
public enum LinkType {

	/**
	 * No link replacing.
	 */
	OFF,

	/**
	 * Link replacing without the API prefix and without the project name.
	 */
	SHORT,

	/**
	 * Link replacing without the API prefix, but with the project name.
	 */
	MEDIUM,

	/**
	 * Link replacing with API prefix and project name.
	 */
	FULL

}

package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Contains global roots of every element type where permissions can be assigned to.
 */
public interface PermissionRoots {

	/**
	 * Return the root for projects.
	 * 
	 * @return
	 */
	HibBaseElement project();

	/**
	 * Return the root for users.
	 * 
	 * @return
	 */
	HibBaseElement user();

	/**
	 * Return the root for groups.
	 * 
	 * @return
	 */
	HibBaseElement group();

	/**
	 * Return the root for roles.
	 * 
	 * @return
	 */
	HibBaseElement role();

	/**
	 * Return the root for microschemas.
	 * 
	 * @return
	 */
	HibBaseElement microschema();

	/**
	 * Return the root for schemas.
	 * 
	 * @return
	 */
	HibBaseElement schema();

	/**
	 * Return the topmost permission level element.
	 * 
	 * @return
	 */
	HibBaseElement mesh();
}

package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Contains global roots of every element type where permissions can be assigned to.
 */
public interface PermissionRoots {

	String PROJECTS = "projects";
	String USERS = "users";
	String GROUPS = "groups";
	String ROLES = "roles";
	String MICROSCHEMAS = "microschemas";
	String SCHEMAS = "schemas";
	String BRANCHES = "branches";
	String TAG_FAMILIES = "tagFamilies";
	String NODES = "nodes";
	String TAGS = "tags";
	String MESH = "mesh";

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

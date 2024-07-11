package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.BaseElement;

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
	BaseElement project();

	/**
	 * Return the root for users.
	 * 
	 * @return
	 */
	BaseElement user();

	/**
	 * Return the root for groups.
	 * 
	 * @return
	 */
	BaseElement group();

	/**
	 * Return the root for roles.
	 * 
	 * @return
	 */
	BaseElement role();

	/**
	 * Return the root for microschemas.
	 * 
	 * @return
	 */
	BaseElement microschema();

	/**
	 * Return the root for schemas.
	 * 
	 * @return
	 */
	BaseElement schema();

	/**
	 * Return the topmost permission level element.
	 * 
	 * @return
	 */
	BaseElement mesh();
}

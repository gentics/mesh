package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Contains global roots of every element type where permissions can be assigned to.
 */
public interface PermissionRoots {

	HibBaseElement project();

	HibBaseElement user();

	HibBaseElement group();

	HibBaseElement role();

	HibBaseElement microschema();

	HibBaseElement schema();
}

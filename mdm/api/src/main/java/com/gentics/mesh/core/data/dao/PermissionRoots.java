package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Contains global roots of every element type where permissions can be assigned to.
 */
// TODO there is a chance for this to get back to mdm-orientdb-api
public interface PermissionRoots {

	HibBaseElement project();

	HibBaseElement user();

	HibBaseElement group();

	HibBaseElement role();

	HibBaseElement microschema();

	HibBaseElement schema();
}

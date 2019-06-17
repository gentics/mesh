package com.gentics.mesh.core.data.changelog;

import java.util.Iterator;

import com.gentics.mesh.core.data.MeshVertex;

public interface ChangelogRoot extends MeshVertex {

	/**
	 * Returns all listed changelog marker vertices.
	 * 
	 * @return
	 */
	Iterator<? extends ChangeMarkerVertex> findAll();

	/**
	 * Check whether the change is already listed.
	 * 
	 * @param change
	 * @return
	 */
	boolean hasChange(Change change);

	/**
	 * Add the change to the list of executed changes.
	 * 
	 * @param change
	 * @param duration
	 */
	void add(Change change, long duration);
}

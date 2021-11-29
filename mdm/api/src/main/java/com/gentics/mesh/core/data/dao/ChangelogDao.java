package com.gentics.mesh.core.data.dao;

import java.util.Iterator;

import com.gentics.mesh.core.data.changelog.Change;
import com.gentics.mesh.core.data.changelog.ChangeMarker;

/**
 * DAO for for @link {@link ChangeMarker} vertices.
 */
public interface ChangelogDao {

	/**
	 * Returns all listed changelog marker vertices.
	 * 
	 * @return
	 */
	Iterator<? extends ChangeMarker> findAll();

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

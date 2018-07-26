package com.gentics.mesh.core.data.changelog;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * Marker vertex which is used to mark the change as executed by storing it in the graph.
 */
public interface ChangeMarkerVertex extends MeshVertex {

	public static String DURATION_KEY = "duration";

	/**
	 * Return the duration of the change.
	 * 
	 * @return
	 */
	default Long getDuration() {
		return getProperty(DURATION_KEY);
	}

	/**
	 * Set the duration of the change.
	 * 
	 * @param duration
	 */
	default void setDuration(long duration) {
		setProperty(DURATION_KEY, duration);
	}
}

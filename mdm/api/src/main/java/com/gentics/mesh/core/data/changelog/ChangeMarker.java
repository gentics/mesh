package com.gentics.mesh.core.data.changelog;

import com.gentics.mesh.core.data.BaseElement;

/**
 * Marker element which is used to mark the change as executed by storing it in the DB.
 */
public interface ChangeMarker extends BaseElement {

	/**
	 * Return the duration of the change.
	 * 
	 * @return
	 */
	Long getDuration();

	/**
	 * Set the duration of the change.
	 * 
	 * @param duration
	 */
	void setDuration(long duration);
}

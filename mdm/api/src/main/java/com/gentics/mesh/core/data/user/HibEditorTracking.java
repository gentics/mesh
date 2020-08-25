package com.gentics.mesh.core.data.user;

import com.gentics.mesh.util.DateUtils;

public interface HibEditorTracking {

	/**
	 * Return the editor of the vertex.
	 * 
	 * @return Editor
	 */
	HibUser getEditor();

	/**
	 * Set the editor of the vertex.
	 * 
	 * @param user
	 *            Editor
	 */
	void setEditor(HibUser user);

	/**
	 * Return the timestamp on which the vertex was last updated.
	 * 
	 * @return Edit timestamp
	 */
	Long getLastEditedTimestamp();

	/**
	 * Return the ISO8601 formatted edited date.
	 * 
	 * @return
	 */
	default String getLastEditedDate() {
		return DateUtils.toISO8601(getLastEditedTimestamp(), 0);
	}

	/**
	 * Set the timestamp on which the vertex was last updated.
	 * 
	 * @param timestamp
	 *            Edit timestamp
	 */
	void setLastEditedTimestamp(long timestamp);

	/**
	 * Update the last edit timestamp using the current time.
	 */
	void setLastEditedTimestamp();

}

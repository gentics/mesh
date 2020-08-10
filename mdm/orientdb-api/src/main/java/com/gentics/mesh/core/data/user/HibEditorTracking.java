package com.gentics.mesh.core.data.user;

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

	/**
	 * Return the ISO8601 formatted edited date.
	 * 
	 * @return
	 */
	String getLastEditedDate();

}

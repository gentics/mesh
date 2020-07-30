package com.gentics.mda.entity;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.util.DateUtils;

/**
 * Interface for vertices that track their editor and editing time
 */
public interface AEditorTrackingVertex extends AMeshElement {

	String LAST_EDIT_TIMESTAMP_PROPERTY_KEY = "last_edited_timestamp";

	/**
	 * Return the editor of the vertex.
	 * 
	 * @return Editor
	 */
	User getEditor();

	/**
	 * Set the editor of the vertex.
	 * 
	 * @param user
	 *            Editor
	 */
	void setEditor(AUser user);

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
	default void setLastEditedTimestamp() {
		setLastEditedTimestamp(System.currentTimeMillis());
	}

	/**
	 * Return the ISO8601 formatted edited date.
	 * 
	 * @return
	 */
	default String getLastEditedDate() {
		return DateUtils.toISO8601(getLastEditedTimestamp(), 0);
	}

}

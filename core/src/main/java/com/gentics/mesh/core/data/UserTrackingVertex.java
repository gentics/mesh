package com.gentics.mesh.core.data;

/**
 * Interface for vertices that track creation and editing
 */
public interface UserTrackingVertex extends CreatorTrackingVertex, EditorTrackingVertex {

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	default void setCreated(User creator) {
		setCreator(creator);
		setCreationTimestamp(System.currentTimeMillis());
		setEditor(creator);
		setLastEditedTimestamp(System.currentTimeMillis());
	}
}

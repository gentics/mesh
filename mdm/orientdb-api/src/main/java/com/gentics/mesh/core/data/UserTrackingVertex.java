package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.user.HibUser;

/**
 * Interface for vertices that track creation and editing.
 */
public interface UserTrackingVertex extends CreatorTrackingVertex, EditorTrackingVertex {

	/**
	 * Set the editor and creator references and update the timestamp for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	default void setCreated(HibUser creator) {
		setCreator(creator);
		setCreationTimestamp();
		setEditor(creator);
		setLastEditedTimestamp();
	}

}

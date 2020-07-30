package com.gentics.mda.entity;

/**
 * Interface for vertices that track creation and editing.
 */
public interface AUserTrackingVertex extends ACreatorTrackingVertex, AEditorTrackingVertex {

	/**
	 * Set the editor and creator references and update the timestamp for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	default void setCreated(AUser creator) {
		setCreator(creator);
		setCreationTimestamp();
		setEditor(creator);
		setLastEditedTimestamp();
	}

}

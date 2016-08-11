package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;

import com.gentics.mesh.core.data.impl.UserImpl;

/**
 * Interface for vertices that track their editor and editing time
 */
public interface EditorTrackingVertex extends MeshVertex {
	static final String LAST_EDIT_TIMESTAMP_PROPERTY_KEY = "last_edited_timestamp";

	/**
	 * Return the editor of the vertex.
	 * 
	 * @return Editor
	 */
	default User getEditor() {
		return getImpl().out(HAS_EDITOR).has(UserImpl.class).nextOrDefaultExplicit(UserImpl.class, null);
	}

	/**
	 * Set the editor of the vertex.
	 * 
	 * @param user
	 *            Editor
	 */
	default void setEditor(User user) {
		getImpl().setLinkOut(user.getImpl(), HAS_EDITOR);
	}

	/**
	 * Return the timestamp on which the vertex was last updated.
	 * 
	 * @return Edit timestamp
	 */
	default Long getLastEditedTimestamp() {
		return getImpl().getProperty(LAST_EDIT_TIMESTAMP_PROPERTY_KEY);
	}

	/**
	 * Set the timestamp on which the vertex was last updated.
	 * 
	 * @param timestamp
	 *            Edit timestamp
	 */
	default void setLastEditedTimestamp(long timestamp) {
		getImpl().setProperty(LAST_EDIT_TIMESTAMP_PROPERTY_KEY, timestamp);
	}

}

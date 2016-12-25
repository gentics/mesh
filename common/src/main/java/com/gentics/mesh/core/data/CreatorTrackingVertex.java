package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;

/**
 * Interface for vertices that track their creator and creation time.
 */
public interface CreatorTrackingVertex extends MeshVertex {

	static final String CREATION_TIMESTAMP_PROPERTY_KEY = "creation_timestamp";

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	User getCreator();

	/**
	 * Set the creator of the vertex.
	 * 
	 * @param user
	 *            Creator
	 */
	default void setCreator(User user) {
		setLinkOut(user, HAS_CREATOR);
	}

	/**
	 * Return the timestamp on which the vertex was created.
	 * 
	 * @return Creation timestamp
	 */
	default Long getCreationTimestamp() {
		return getProperty(CREATION_TIMESTAMP_PROPERTY_KEY);
	}

	/**
	 * Set the timestamp on which the vertex was created.
	 * 
	 * @param timestamp
	 *            Creation timestamp
	 */
	default void setCreationTimestamp(long timestamp) {
		setProperty(CREATION_TIMESTAMP_PROPERTY_KEY, timestamp);
	}

	/**
	 * Set the creation timestamp using the current time.
	 */
	default void setCreationTimestamp() {
		setCreationTimestamp(System.currentTimeMillis());
	}

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	default void setCreated(User creator) {
		setCreator(creator);
		setCreationTimestamp(System.currentTimeMillis());
	}
}

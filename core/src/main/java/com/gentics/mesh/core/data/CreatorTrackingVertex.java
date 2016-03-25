package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;

import com.gentics.mesh.core.data.impl.UserImpl;

/**
 * Interface for vertices that track their creator and creation time
 */
public interface CreatorTrackingVertex extends MeshVertex {
	static final String CREATION_TIMESTAMP_PROPERTY_KEY = "creation_timestamp";

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	default User getCreator() {
		return getImpl().out(HAS_CREATOR).has(UserImpl.class).nextOrDefault(UserImpl.class, null);
	}

	/**
	 * Set the creator of the vertex.
	 * 
	 * @param user
	 *            Creator
	 */
	default void setCreator(User user) {
		getImpl().setLinkOut(user.getImpl(), HAS_CREATOR);
	}

	/**
	 * Return the timestamp on which the vertex was created.
	 * 
	 * @return Creation timestamp
	 */
	default Long getCreationTimestamp() {
		return getImpl().getProperty(CREATION_TIMESTAMP_PROPERTY_KEY);
	}

	/**
	 * Set the timestamp on which the vertex was created.
	 * 
	 * @param timestamp
	 *            Creation timestamp
	 */
	default void setCreationTimestamp(long timestamp) {
		getImpl().setProperty(CREATION_TIMESTAMP_PROPERTY_KEY, timestamp);
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

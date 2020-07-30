package com.gentics.mda.entity;


import com.gentics.mesh.util.DateUtils;

/**
 * Interface for vertices that track their creator and creation time.
 */
public interface ACreatorTrackingVertex extends AMeshElement {

	static final String CREATION_TIMESTAMP_PROPERTY_KEY = "creation_timestamp";

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	AUser getCreator();

	/**
	 * Set the creator of the vertex.
	 * 
	 * @param user
	 *            Creator
	 */
	void setCreator(AUser user);

	/**
	 * Return the timestamp on which the vertex was created.
	 * 
	 * @return Creation timestamp
	 */
	Long getCreationTimestamp();

	/**
	 * Return the ISO 8601 formatted creation date.
	 * 
	 * @return
	 */
	default String getCreationDate() {
		return DateUtils.toISO8601(getCreationTimestamp(), 0);
	}

	/**
	 * Set the timestamp on which the vertex was created.
	 * 
	 * @param timestamp
	 *            Creation timestamp
	 */
	void setCreationTimestamp(long timestamp);

	/**
	 * Set the creation timestamp using the current time.
	 */
	void setCreationTimestamp();

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	default void setCreated(AUser creator) {
		setCreator(creator);
		setCreationTimestamp(System.currentTimeMillis());
	}

}

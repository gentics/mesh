package com.gentics.mesh.core.data.user;

import com.gentics.mesh.util.DateUtils;

/**
 * Domain model extension for elements which have a creator.
 */
public interface HibCreatorTracking {

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	HibUser getCreator();

	/**
	 * Set the creator of the element.
	 * 
	 * @param user
	 */
	void setCreator(HibUser user);

	/**
	 * Set the editor and creator references and update the timestamps for created and edited fields.
	 * 
	 * @param creator
	 *            Creator
	 */
	void setCreated(HibUser creator);

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
	 * Return the ISO 8601 formatted creation date.
	 * 
	 * @return
	 */
	default String getCreationDate() {
		return DateUtils.toISO8601(getCreationTimestamp(), 0);
	}

	/**
	 * Return the creation timestamp.
	 * 
	 * @return
	 */
	Long getCreationTimestamp();

}

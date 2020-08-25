package com.gentics.mesh.core.data.user;

import com.gentics.mesh.util.DateUtils;

public interface HibCreatorTracking {

	/**
	 * Return the creator of the vertex.
	 * 
	 * @return Creator
	 */
	HibUser getCreator();

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


	Long getCreationTimestamp();

}

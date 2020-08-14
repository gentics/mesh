package com.gentics.mesh.core.data.user;

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

	Long getCreationTimestamp();

}

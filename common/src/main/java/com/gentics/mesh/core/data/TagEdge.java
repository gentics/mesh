package com.gentics.mesh.core.data;

/**
 * Interface for edges between nodes and tags. The edges are release specific.
 */
public interface TagEdge {
	/**
	 * Get the release Uuid
	 * @return release Uuid
	 */
	String getReleaseUuid();

	/**
	 * Set the release Uuid
	 * @param uuid release Uuid
	 */
	void setReleaseUuid(String uuid);

}

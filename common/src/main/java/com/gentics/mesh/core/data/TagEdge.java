package com.gentics.mesh.core.data;

/**
 * Interface for edges between nodes and tags. The edges are branch specific.
 */
public interface TagEdge extends MeshEdge {

	/**
	 * Get the branch Uuid
	 * 
	 * @return branch Uuid
	 */
	String getBranchUuid();

	/**
	 * Set the branch Uuid
	 * 
	 * @param uuid
	 *            branch Uuid
	 */
	void setBranchUuid(String uuid);

}

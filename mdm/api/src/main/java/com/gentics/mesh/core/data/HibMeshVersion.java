package com.gentics.mesh.core.data;

public interface HibMeshVersion {

	/**
	 * Returns the mesh version which was last used to access the graph. This version is usually updated during startup of mesh.
	 *
	 * @return Plain text version or null if this information has not yet been stored
	 */
	String getMeshVersion();

	/**
	 * Set the plain text mesh version which was last used to access the graph.
	 *
	 * @param version
	 */
	void setMeshVersion(String version);

	/**
	 * Return the currently stored database revision hash.
	 *
	 * @return
	 */
	String getDatabaseRevision();

	/**
	 * Update the stored database revision hash.
	 *
	 * @param databaseRevision
	 */
	void setDatabaseRevision(String databaseRevision);
}

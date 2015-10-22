package com.gentics.mesh.etc;

/**
 * Search engine options POJO.
 */
public class ElasticSearchOptions {

	public static final String DEFAULT_DIRECTORY = "mesh-searchindex";

	private String directory = DEFAULT_DIRECTORY;

	/**
	 * 
	 * Return the search index filesystem directory.
	 * 
	 * @return Path to the search index filesystem directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Set the search index filesystem directory.
	 * 
	 * @param directory
	 *            Path to the search index filesystem directory
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

}

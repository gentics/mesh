package com.gentics.mesh.etc;

import java.io.File;

/**
 * Search engine options POJO.
 */
public class ElasticSearchOptions {

	public static final String DEFAULT_DIRECTORY = "data" + File.separator + "searchindex";

	private String directory = DEFAULT_DIRECTORY;

	private boolean httpEnabled = false;

	/**
	 * Check whether the http server should be enabled.
	 * 
	 * @return
	 */
	public boolean isHttpEnabled() {
		return httpEnabled;
	}

	/**
	 * Set http server flag
	 * 
	 * @param httpEnabled
	 *            Server flag
	 */
	public void setHttpEnabled(boolean httpEnabled) {
		this.httpEnabled = httpEnabled;
	}

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

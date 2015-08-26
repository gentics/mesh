package com.gentics.mesh.etc;

public class ElasticSearchOptions {
	public static final String DEFAULT_DIRECTORY = "mesh-searchindex";

	private String directory = DEFAULT_DIRECTORY;

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

}

package com.gentics.mesh.parameter;

/**
 * Image manipulation retrieval parameters.
 * 
 * @author plyhun
 *
 */
public interface ImageManipulationRetrievalParameters extends ParameterProvider {

	public static final String FILESIZE_QUERY_PARAM_KEY = "filesize";

	/**
	 * Set if filesizes should also be retrieved.
	 * 
	 * @param retrieveFilesize
	 * @return
	 */
	default ImageManipulationRetrievalParameters setRetrieveFilesize(boolean retrieveFilesize) {
		setParameter(FILESIZE_QUERY_PARAM_KEY, Boolean.toString(retrieveFilesize));
		return this;
	}

	/**
	 * Check if filesizes should also be retrieved.
	 * 
	 * @return
	 */
	default boolean retrieveFilesize() {
		String filesize = getParameter(FILESIZE_QUERY_PARAM_KEY);
		return filesize != null ? Boolean.parseBoolean(filesize) : false;
	}
}

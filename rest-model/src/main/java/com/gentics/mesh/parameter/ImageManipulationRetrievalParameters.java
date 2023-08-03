package com.gentics.mesh.parameter;

/**
 * Image manipulation retrieval parameters.
 * 
 * @author plyhun
 *
 */
public interface ImageManipulationRetrievalParameters extends ParameterProvider {

	public static final String FILESIZE_QUERY_PARAM_KEY = "filesize";
	public static final String ORIGINAL_QUERY_PARAM_KEY = "original";

	/**
	 * Set whether filesizes should also be retrieved.
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

	/**
	 * Set whether original image should also be retrieved.
	 * 
	 * @param retrieveOriginal
	 * @return
	 */
	default ImageManipulationRetrievalParameters setRetrieveOriginal(boolean retrieveOriginal) {
		setParameter(ORIGINAL_QUERY_PARAM_KEY, Boolean.toString(retrieveOriginal));
		return this;
	}

	/**
	 * Check if original image should also be retrieved.
	 * 
	 * @return
	 */
	default boolean retrieveOriginal() {
		String original = getParameter(ORIGINAL_QUERY_PARAM_KEY);
		return original != null ? Boolean.parseBoolean(original) : false;
	}
}

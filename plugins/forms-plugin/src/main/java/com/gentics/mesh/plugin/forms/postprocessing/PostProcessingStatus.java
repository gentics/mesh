package com.gentics.mesh.plugin.forms.postprocessing;

/**
 * Status of a post processing action for a form data node
 */
public enum PostProcessingStatus {
	/**
	 * Action needs to be performed (initially or as retry)
	 */
	PENDING,

	/**
	 * Action is currently performed
	 */
	RUNNING,

	/**
	 * Action was performed successfully
	 */
	DONE,

	/**
	 * Action failed and the maximum number of attempts is reached
	 */
	FAILED
}

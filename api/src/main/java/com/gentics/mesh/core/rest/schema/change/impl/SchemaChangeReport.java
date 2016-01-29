package com.gentics.mesh.core.rest.schema.change.impl;

/**
 * Rest Model POJO for a schema change report.
 */
public class SchemaChangeReport {

	private int affectedNodes;
	private long duration;
	private String error;
	private boolean success = false;

	/**
	 * Return the count of nodes which were affected by this change.
	 * 
	 * @return
	 */
	public int getAffectedNodes() {
		return affectedNodes;
	}

	/**
	 * Set the count of nodes which were affected by this change.
	 * 
	 * @param affectedNodes
	 */
	public void setAffectedNodes(int affectedNodes) {
		this.affectedNodes = affectedNodes;
	}

	/**
	 * Return the duration it took to execute the change.
	 * 
	 * @return
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Set the execution duration.
	 * 
	 * @param durationInMs
	 */
	public void setDuration(long durationInMs) {
		this.duration = durationInMs;
	}

	/**
	 * Return the error message.
	 * 
	 * @return
	 */
	public String getError() {
		return error;
	}

	/**
	 * Set the error message.
	 * 
	 * @param error
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Return the success flag for the applied change within the report.
	 * 
	 * @return
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Set the success flag for the report.
	 * 
	 * @param success
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

}

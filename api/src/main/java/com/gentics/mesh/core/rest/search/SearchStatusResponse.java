package com.gentics.mesh.core.rest.search;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model POJO for a search status response.
 */
public class SearchStatusResponse implements RestModel {

	long batchCount;

	/**
	 * Return the current batch count within the search queue.
	 * 
	 * @return
	 */
	public long getBatchCount() {
		return batchCount;
	}

	/**
	 * Set the batch count value for the response.
	 * 
	 * @param batchCount
	 */
	public void setBatchCount(long batchCount) {
		this.batchCount = batchCount;
	}
}

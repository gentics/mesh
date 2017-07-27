package com.gentics.mesh.core.rest.node;

/**
 * Webroot response which is a container for either a node response or a download response. The webroot endpoint can return either a node response or a binary
 * field download.
 */
public class WebRootResponse {

	private NodeResponse nodeResponse;

	private NodeDownloadResponse downloadResponse;

	/**
	 * Check whether the webroot response contains a stored download response.
	 * 
	 * @return
	 */
	public boolean isDownload() {
		return downloadResponse != null;
	}

	/**
	 * Get the stored download response.
	 * 
	 * @return
	 */
	public NodeDownloadResponse getDownloadResponse() {
		return downloadResponse;
	}

	/**
	 * Set the stored download response.
	 * 
	 * @param downloadResponse
	 */
	public void setDownloadResponse(NodeDownloadResponse downloadResponse) {
		this.downloadResponse = downloadResponse;
	}

	/**
	 * Return the stored node response.
	 * 
	 * @return
	 */
	public NodeResponse getNodeResponse() {
		return nodeResponse;
	}

	/**
	 * Set the stored node response.
	 * 
	 * @param nodeResponse
	 */
	public void setNodeResponse(NodeResponse nodeResponse) {
		this.nodeResponse = nodeResponse;
	}

}

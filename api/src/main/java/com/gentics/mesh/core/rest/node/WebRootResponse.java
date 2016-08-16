package com.gentics.mesh.core.rest.node;

public class WebRootResponse {

	private NodeResponse nodeResponse;

	private NodeDownloadResponse downloadResponse;

	public boolean isBinary() {
		return downloadResponse != null;
	}

	public NodeDownloadResponse getDownloadResponse() {
		return downloadResponse;
	}

	public void setDownloadResponse(NodeDownloadResponse downloadResponse) {
		this.downloadResponse = downloadResponse;
	}

	public NodeResponse getNodeResponse() {
		return nodeResponse;
	}

	public void setNodeResponse(NodeResponse nodeResponse) {
		this.nodeResponse = nodeResponse;
	}

}

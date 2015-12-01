package com.gentics.mesh.core.rest.node;

public class WebRootResponse {

	private NodeResponse nodeResponse;

	private NodeDownloadResponse downloadResponse;

	public WebRootResponse(Object response) {
		if (response instanceof NodeResponse) {
			nodeResponse = (NodeResponse) response;
		} else if (response instanceof NodeDownloadResponse) {
			downloadResponse = (NodeDownloadResponse) response;
		} else {
			throw new RuntimeException("Unknown response type {" + response.getClass().getName() + "}");
		}
	}

	public boolean isBinary() {
		return downloadResponse != null;
	}

	public NodeDownloadResponse getDownloadResponse() {
		return downloadResponse;
	}

	public NodeResponse getNodeResponse() {
		return nodeResponse;
	}
}

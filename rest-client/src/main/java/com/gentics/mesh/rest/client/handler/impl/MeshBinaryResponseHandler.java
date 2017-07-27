package com.gentics.mesh.rest.client.handler.impl;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

/**
 * Handler which can handle download responses.
 */
public class MeshBinaryResponseHandler extends ModelResponseHandler<NodeDownloadResponse> {

	/**
	 * Create a new response handler.
	 * 
	 * @param method
	 *            Method that was used for the request
	 * @param uri
	 *            URI that was queried
	 */
	public MeshBinaryResponseHandler(HttpMethod method, String uri) {
		super(NodeDownloadResponse.class, method, uri);
	}

	@Override
	public void handleSuccess(HttpClientResponse rh) {
		NodeDownloadResponse response = new NodeDownloadResponse();
		String contentType = rh.getHeader(HttpHeaders.CONTENT_TYPE.toString());
		response.setContentType(contentType);
		String disposition = rh.getHeader("content-disposition");
		String filename = disposition.substring(disposition.indexOf("=") + 1);
		response.setFilename(filename);

		rh.bodyHandler(buffer -> {
			response.setBuffer(buffer);
			future.complete(response);
		});
	}

}

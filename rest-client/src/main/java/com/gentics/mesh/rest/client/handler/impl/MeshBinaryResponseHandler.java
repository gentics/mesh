package com.gentics.mesh.rest.client.handler.impl;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.rest.client.handler.AbstractMeshResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

/**
 * Handler which can handle download responses.
 */
public class MeshBinaryResponseHandler extends AbstractMeshResponseHandler<NodeDownloadResponse> {

	/**
	 * Create a new response handler.
	 * 
	 * @param classOfT
	 *            Expected response POJO class
	 * @param method
	 *            Method that was used for the request
	 * @param uri
	 *            Uri that was queried
	 */
	public MeshBinaryResponseHandler(HttpMethod method, String uri) {
		super(method, uri);
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

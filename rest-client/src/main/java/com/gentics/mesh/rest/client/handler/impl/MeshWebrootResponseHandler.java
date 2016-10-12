package com.gentics.mesh.rest.client.handler.impl;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.rest.client.handler.AbstractMeshResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

/**
 * Response handler for webroot responses. The webroot endpoint is special since it may return a JSON response or the binary field value. This handler will wrap
 * both possible values into one {@link WebRootResponse} object.
 */
public class MeshWebrootResponseHandler extends AbstractMeshResponseHandler<WebRootResponse> {

	public MeshWebrootResponseHandler(HttpMethod method, String uri) {
		super(method, uri);
	}

	@Override
	public void handleSuccess(HttpClientResponse rh) {

		WebRootResponse response = new WebRootResponse();
		NodeDownloadResponse downloadResponse = new NodeDownloadResponse();
		String contentType = rh.getHeader(HttpHeaders.CONTENT_TYPE.toString());
		if (contentType.startsWith(APPLICATION_JSON)) {
			// Delegate the response to the json handler
			MeshJsonResponseHandler<NodeResponse> handler = new MeshJsonResponseHandler<>(NodeResponse.class, getMethod(), contentType);
			handler.handle(rh);
			handler.getFuture().setHandler(rh2 -> {
				if (rh2.failed()) {
					future.fail(rh2.cause());
				} else {
					response.setNodeResponse(rh2.result());
					future.complete(response);
				}
			});
		} else {
			downloadResponse.setContentType(contentType);
			String disposition = rh.getHeader("content-disposition");
			String filename = disposition.substring(disposition.indexOf("=") + 1);
			downloadResponse.setFilename(filename);

			rh.bodyHandler(buffer -> {
				downloadResponse.setBuffer(buffer);
				response.setDownloadResponse(downloadResponse);
				future.complete(response);
			});
		}
	}

}

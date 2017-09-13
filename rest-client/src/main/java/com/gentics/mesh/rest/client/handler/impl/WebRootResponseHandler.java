package com.gentics.mesh.rest.client.handler.impl;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.http.MeshHeaders;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * Response handler for webroot responses. The webroot endpoint is special since it may return a JSON response or the binary field value. This handler will wrap
 * both possible values into one {@link WebRootResponse} object.
 */
public class WebRootResponseHandler extends ModelResponseHandler<WebRootResponse> {

	public WebRootResponseHandler(HttpMethod method, String uri) {
		super(WebRootResponse.class, method, uri);
	}

	@Override
	public void handleSuccess(HttpClientResponse rh) {

		WebRootResponse response = new WebRootResponse();
		NodeDownloadResponse downloadResponse = new NodeDownloadResponse();
		String contentType = rh.getHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE);
		if (contentType == null) {
			future.fail("The {" + MeshHeaders.WEBROOT_RESPONSE_TYPE + "} header was not set correctly.");
			return;
		}
		if (contentType.startsWith("node")) {
			// Delegate the response to the json handler
			ModelResponseHandler<NodeResponse> handler = new ModelResponseHandler<>(NodeResponse.class, getMethod(), contentType);
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

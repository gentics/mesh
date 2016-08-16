package com.gentics.mesh.rest.client.handler.impl;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.rest.client.handler.AbstractMeshResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;

public class MeshWebrootResponseHandler extends AbstractMeshResponseHandler<WebRootResponse> {

	public MeshWebrootResponseHandler(HttpMethod method, String uri) {
		super(method, uri);
	}

	@Override
	public void handle(HttpClientResponse rh) {

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

		//		MeshResponse<WebRootResponse> future = MeshResponse.create();
		//		handler.getFuture().setHandler(rh -> {
		//			if (rh.failed()) {
		//				future.fail(rh.cause());
		//			} else {
		//				future.complete(new WebRootResponse(rh.result()));
		//			}
		//		});

	}

}

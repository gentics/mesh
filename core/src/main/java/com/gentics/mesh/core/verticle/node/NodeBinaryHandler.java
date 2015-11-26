package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.File;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

public class NodeBinaryHandler implements Handler<Node> {

	private RoutingContext rc;
	private ImageManipulator imageManipulator;

	public NodeBinaryHandler(RoutingContext rc, ImageManipulator imageManipulator) {
		this.rc = rc;
		this.imageManipulator = imageManipulator;
	}

	@Override
	public void handle(Node node) {
		if (!node.getSchema().isBinary()) {
			rc.fail(error(NOT_FOUND, "node_error_no_binary_node"));
			return;
		}
		File binaryFile = node.getBinaryFile();
		if (!binaryFile.exists()) {
			rc.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
			return;
		} else {
			InternalActionContext ac = InternalActionContext.create(rc);
			String contentLength = String.valueOf(node.getBinaryFileSize());
			String fileName = node.getBinaryFileName();
			String contentType = node.getBinaryContentType();
			// Resize the image if needed
			if (node.hasBinaryImage() && ac.getImageRequestParameter().isSet()) {
				Observable<io.vertx.rxjava.core.buffer.Buffer> buffer = imageManipulator.handleResize(node.getBinaryFile(), node.getBinarySHA512Sum(),
						ac.getImageRequestParameter());
				buffer.subscribe(imageBuffer -> {
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(imageBuffer.length()));
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
					// TODO encode filename?
					rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);
					rc.response().end((Buffer) imageBuffer.getDelegate());
				} , error -> {
					rc.fail(error);
				});
			} else {
				node.getBinaryFileBuffer().setHandler(bh -> {
					Buffer buffer = bh.result();
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
					// TODO encode filename?
					rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);
					rc.response().end(buffer);
				});
			}
		}

	}

}

package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

import java.io.File;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.util.ETag;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import rx.Single;

/**
 * Handler which will accept {@link BinaryGraphField} elements and return the binary data using the given context.
 */
public class BinaryFieldResponseHandler implements Handler<BinaryGraphField> {

	private RoutingContext rc;
	private ImageManipulator imageManipulator;

	public BinaryFieldResponseHandler(RoutingContext rc, ImageManipulator imageManipulator) {
		this.rc = rc;
		this.imageManipulator = imageManipulator;
	}

	@Override
	public void handle(BinaryGraphField binaryField) {
		File binaryFile = binaryField.getFile();
		if (!binaryFile.exists()) {
			rc.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
			return;
		} else {
			InternalActionContext ac = InternalActionContext.create(rc);
			String contentLength = String.valueOf(binaryField.getFileSize());
			String fileName = binaryField.getFileName();
			String contentType = binaryField.getMimeType();
			String sha512sum = binaryField.getSHA512Sum();

			// Check the etag
			String etagKey = sha512sum;
			if (binaryField.hasImage() && ac.getImageParameters().isSet()) {
				etagKey += ac.getImageParameters().getQueryParameters();
			}

			String etagHeaderValue = ETag.prepareHeader(ETag.hash(etagKey), false);
			rc.response().putHeader(ETAG, etagHeaderValue);
			String requestETag = rc.request().getHeader(HttpHeaders.IF_NONE_MATCH);
			if (requestETag != null && requestETag.equals(etagHeaderValue)) {
				rc.response().setStatusCode(NOT_MODIFIED.code()).end();
			} else if (binaryField.hasImage() && ac.getImageParameters().isSet()) {
				// Resize the image if needed
				Single<io.vertx.rxjava.core.buffer.Buffer> buffer = imageManipulator.handleResize(binaryField.getFile(), binaryField.getSHA512Sum(),
						ac.getImageParameters());
				buffer.subscribe(imageBuffer -> {
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(imageBuffer.length()));
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
					rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					// TODO encode filename?
					rc.response().putHeader("content-disposition", "inline; filename=" + fileName);
					rc.response().end((Buffer) imageBuffer.getDelegate());
				}, error -> {
					rc.fail(error);
				});
			} else {
				binaryField.getFileBuffer().setHandler(bh -> {
					Buffer buffer = bh.result();
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
					rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					// TODO encode filename?
					// TODO images and pdf files should be shown in inline format 
					rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);
					rc.response().end(buffer);
				});
			}
		}

	}

}

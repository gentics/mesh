package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.ETag;

import io.vertx.core.file.AsyncFile;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler which will accept {@link BinaryGraphField} elements and return the binary data using the given context.
 */
@Singleton
public class BinaryFieldResponseHandler {

	private ImageManipulator imageManipulator;

	private BinaryStorage storage;

	@Inject
	public BinaryFieldResponseHandler(ImageManipulator imageManipulator, BinaryStorage storage) {
		this.imageManipulator = imageManipulator;
		this.storage = storage;
	}

	/**
	 * Handle the binary field response.
	 * 
	 * @param rc
	 * @param binaryField
	 */
	public void handle(RoutingContext rc, BinaryGraphField binaryField) {
		if (!storage.exists(binaryField)) {
			rc.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
			return;
		} else {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String contentLength = String.valueOf(binaryField.getSize());
			String fileName = binaryField.getFileName();
			String contentType = binaryField.getMimeType();
			String sha512sum = binaryField.getBinary()!= null ? binaryField.getBinary().getSHA512Sum() : null;

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
				imageManipulator.handleResize(binaryField, ac.getImageParameters()).subscribe(fileWithProps -> {
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileWithProps.getProps().size()));
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
					rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
					// TODO encode filename?
					rc.response().putHeader("content-disposition", "inline; filename=" + fileName);
					AsyncFile file = fileWithProps.getFile();
					file.endHandler(ignore -> {
						rc.response().end();
						file.close();
					});
					Pump.pump(file, rc.response()).start();
				}, rc::fail);
			} else {
				binaryField.getBinary().getStream().subscribe(file -> {
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
					rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
					// TODO encode filename?
					// TODO images and pdf files should be shown in inline format
					rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);

					file.endHandler(ignore -> {
						rc.response().end();
						file.close();
					});
					Pump.pump(file, rc.response()).start();
				}, rc::fail);
			}
		}

	}

}

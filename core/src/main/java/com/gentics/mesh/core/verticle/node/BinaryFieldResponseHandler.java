package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.ETag;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rxjava.core.http.HttpServerResponse;
import rx.Observable;

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
			Binary binary = binaryField.getBinary();
			String contentLength = String.valueOf(binary.getSize());
			String fileName = binaryField.getFileName();
			String contentType = binaryField.getMimeType();
			String sha512sum = binary.getSHA512Sum();

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
				Observable<Buffer> data = storage.read(sha512sum);
				imageManipulator.handleResize(data, sha512sum, ac.getImageParameters()).subscribe(fileWithProps -> {
					rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileWithProps.getProps().size()));
					rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
					rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
					// TODO encode filename?
					rc.response().putHeader("content-disposition", "inline; filename=" + fileName);
					AsyncFile file = fileWithProps.getFile();
					Pump pump = Pump.pump(file, rc.response());
					file.endHandler(ignore -> {
						rc.response().end();
						file.close();
					});
					pump.start();
				}, rc::fail);
			} else {
				Observable<io.vertx.rxjava.core.buffer.Buffer> stream = binary.getStream().map(buf -> {
					return new io.vertx.rxjava.core.buffer.Buffer(buf);
				});
				rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
				rc.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
				rc.response().putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
				rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
				// TODO encode filename?
				// TODO images and pdf files should be shown in inline format
				rc.response().putHeader("content-disposition", "attachment; filename=" + fileName);

				stream.reduce(rc.response(), (response, buf) -> {
					response.write(buf.getDelegate());
					return response;
				}).toCompletable().subscribe(() -> {
					rc.response().end();
				}, rc::fail);
			}
		}
	}

}

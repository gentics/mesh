package com.gentics.mesh.core.endpoint.node;

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
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.handler.impl.RangeRequestHandlerImpl;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.MimeMapping;
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
		rc.response().putHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
		if (!storage.exists(binaryField)) {
			rc.fail(error(NOT_FOUND, "node_error_binary_data_not_found"));
			return;
		} else {
			if (checkETag(rc, binaryField)) {
				return;
			}
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			ImageManipulationParameters imageParams = ac.getImageParameters();
			if (binaryField.hasProcessableImage() && imageParams.hasResizeParams()) {
				resizeAndRespond(rc, binaryField, imageParams);
			} else {
				respond(rc, binaryField);
			}
		}
	}

	private boolean checkETag(RoutingContext rc, BinaryGraphField binaryField) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String sha512sum = binaryField.getBinary().getSHA512Sum();
		String etagKey = sha512sum;
		if (binaryField.hasProcessableImage()) {
			etagKey += ac.getImageParameters().getQueryParameters();
		}

		String etagHeaderValue = ETag.prepareHeader(ETag.hash(etagKey), false);
		HttpServerResponse response = rc.response();
		response.putHeader(ETAG, etagHeaderValue);
		String requestETag = rc.request().getHeader(HttpHeaders.IF_NONE_MATCH);
		if (requestETag != null && requestETag.equals(etagHeaderValue)) {
			response.setStatusCode(NOT_MODIFIED.code()).end();
			return true;
		}
		return false;
	}

	private void respond(RoutingContext rc, BinaryGraphField binaryField) {
		HttpServerResponse response = rc.response();

		Binary binary = binaryField.getBinary();
		String fileName = binaryField.getFileName();
		String contentType = binaryField.getMimeType();
		// Try to guess the contenttype via the filename
		if (contentType == null) {
			contentType = MimeMapping.getMimeTypeForFilename(fileName);
		}

		response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
		// TODO encode filename?
		// TODO images and pdf files should be shown in inline format
		response.putHeader("content-disposition", "attachment; filename=" + fileName);

		// Set to IDENTITY to avoid gzip compression
		response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);

		String localPath = storage.getLocalPath(binary.getUuid());
		if (localPath != null) {
			RangeRequestHandler handler = new RangeRequestHandlerImpl();
			handler.handle(rc, localPath, contentType);
		} else {
			String contentLength = String.valueOf(binary.getSize());
			if (contentType != null) {
				response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
			}
			response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
			response.putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
			binary.getStream().subscribe(response::write, rc::fail, response::end);
		}

	}

	private void resizeAndRespond(RoutingContext rc, BinaryGraphField binaryField, ImageManipulationParameters imageParams) {
		HttpServerResponse response = rc.response();
		Binary binary = binaryField.getBinary();
		String sha512sum = binary.getSHA512Sum();
		String fileName = binaryField.getFileName();
		// We can maybe enhance the parameters using stored parameters.
		if (!imageParams.hasFocalPoint()) {
			FocalPoint fp = binaryField.getImageFocalPoint();
			if (fp != null) {
				imageParams.setFocalPoint(fp);
			}
		}
		// Resize the image if needed
		Flowable<Buffer> data = binary.getStream();
		Flowable<Buffer> resizedData = imageManipulator.handleResize(data, sha512sum, imageParams)
			.toFlowable()
			.map(fileWithProps -> {
				response.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileWithProps.getProps().size()));
				response.putHeader(HttpHeaders.CONTENT_TYPE, fileWithProps.getMimeType());
				response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
				response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
				// Set to IDENTITY to avoid gzip compression
				response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);

				// TODO encode filename?
				response.putHeader("content-disposition", "inline; filename=" + fileName);
				return fileWithProps.getFile();
			}).flatMap(RxUtil::toBufferFlow);
		resizedData.subscribe(response::write, rc::fail, response::end);

	}

}

package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MimeTypeUtils.DEFAULT_BINARY_MIME_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.EncodeUtil;
import com.gentics.mesh.util.MimeTypeUtils;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;

/**
 * Handler which will accept {@link HibBinaryField} elements and return the binary data using the given context.
 */
@Singleton
public class BinaryFieldResponseHandler {

	private final ImageManipulator imageManipulator;

	private final BinaryStorage storage;

	private final Vertx rxVertx;

	private final RangeRequestHandler rangeRequestHandler;

	@Inject
	public BinaryFieldResponseHandler(ImageManipulator imageManipulator, BinaryStorage storage, Vertx rxVertx, RangeRequestHandler rangeRequestHandler) {
		this.imageManipulator = imageManipulator;
		this.storage = storage;
		this.rxVertx = rxVertx;
		this.rangeRequestHandler = rangeRequestHandler;
	}

	/**
	 * Handle the binary field response.
	 *
	 * @param rc
	 * @param binaryField
	 */
	public void handle(RoutingContext rc, HibBinaryField binaryField) {
		rc.response().putHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
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

	private boolean checkETag(RoutingContext rc, HibBinaryField binaryField) {
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

	private void respond(RoutingContext rc, HibBinaryField binaryField) {
		BinaryDao binaryDao = Tx.get().binaryDao();
		HttpServerResponse response = rc.response();

		HibBinary binary = binaryField.getBinary();
		String fileName = binaryField.getFileName();
		String contentType = binaryField.getMimeType();
		// Try to guess the contenttype via the filename
		if (contentType == null) {
			contentType = MimeMapping.getMimeTypeForFilename(fileName);
		}

		response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");

		addContentDispositionHeader(response, fileName, "attachment");

		// Set to IDENTITY to avoid gzip compression
		response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);

		String localPath = storage.getLocalPath(binary.getUuid());
		if (localPath != null) {
			rangeRequestHandler.handle(rc, localPath, contentType);
		} else {
			String contentLength = String.valueOf(binary.getSize());
			if (contentType != null) {
				response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
			}
			response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
			response.putHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
			binaryDao.getStream(binary).subscribe(response::write, rc::fail, response::end);
		}

	}

	private void resizeAndRespond(RoutingContext rc, HibBinaryField binaryField, ImageManipulationParameters imageParams) {
		HttpServerResponse response = rc.response();
		// We can maybe enhance the parameters using stored parameters.
		if (!imageParams.hasFocalPoint()) {
			FocalPoint fp = binaryField.getImageFocalPoint();
			if (fp != null) {
				imageParams.setFocalPoint(fp);
			}
		}
		Integer originalHeight = binaryField.getBinary().getImageHeight();
		Integer originalWidth = binaryField.getBinary().getImageWidth();

		if ("auto".equals(imageParams.getHeight())) {
			imageParams.setHeight(originalHeight);
		}
		if ("auto".equals(imageParams.getWidth())) {
			imageParams.setWidth(originalWidth);
		}
		String fileName = binaryField.getFileName();
		imageManipulator.handleResize(binaryField.getBinary(), imageParams)
			.flatMap(cachedFilePath -> rxVertx.fileSystem().rxProps(cachedFilePath)
				.doOnSuccess(props -> {
					response.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(props.size()));
					response.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.getMimeTypeForFilename(cachedFilePath).orElse(DEFAULT_BINARY_MIME_TYPE));
					response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
					response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
					// Set to IDENTITY to avoid gzip compression
					response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);

					addContentDispositionHeader(response, fileName, "inline");

					response.sendFile(cachedFilePath);
				}))
			.subscribe(ignore -> {}, rc::fail);
	}

	private void addContentDispositionHeader(HttpServerResponse response, String fileName, String type) {
		String encodedFileNameUTF8 = EncodeUtil.encodeForRFC5597(fileName);
		String encodedFileNameISO = EncodeUtil.toISO88591(fileName);

		StringBuilder value = new StringBuilder();
		value.append(type + ";");
		value.append(" filename=\"" + encodedFileNameISO + "\";");
		value.append(" filename*=" + encodedFileNameUTF8);
		response.putHeader("content-disposition", value.toString());
	}

}

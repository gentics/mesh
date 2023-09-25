package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MimeTypeUtils.DEFAULT_BINARY_MIME_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.dao.PersistingBinaryDao;
import com.gentics.mesh.core.data.dao.PersistingImageVariantDao;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.ImageManipulationParameters;
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
	private final ImageManipulatorOptions options;
	private final PersistingBinaryDao binaryDao;
	private final PersistingImageVariantDao imageVariantDao;

	@Inject
	public BinaryFieldResponseHandler(ImageManipulator imageManipulator, BinaryStorage storage, Vertx rxVertx, RangeRequestHandler rangeRequestHandler, MeshOptions options, PersistingBinaryDao binaryDao, PersistingImageVariantDao imageVariantDao) {
		this.imageManipulator = imageManipulator;
		this.storage = storage;
		this.rxVertx = rxVertx;
		this.rangeRequestHandler = rangeRequestHandler;
		this.binaryDao = binaryDao;
		this.imageVariantDao = imageVariantDao;
		this.options = options.getImageOptions();
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

	private void respond(RoutingContext rc, HibImageDataElement binary, String fileName, String contentType) {
		HttpServerResponse response = rc.response();

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

	private void respond(RoutingContext rc, HibBinaryField binaryField) {
		HibBinary binary = binaryField.getBinary();
		String fileName = binaryField.getFileName();
		String contentType = binaryField.getMimeType();

		respond(rc, binary, fileName, contentType);
	}

	private void resizeAndRespond(RoutingContext rc, HibBinaryField binaryField, ImageManipulationParameters imageParams) {
		HttpServerResponse response = rc.response();
		// We can maybe enhance the parameters using stored parameters.
		String fileName = binaryField.getFileName();
		ImageManipulationMode mode = options.getMode();

		switch (mode) {
		case OFF:
			throw error(BAD_REQUEST, "image_error_turned_off");
		case ON_DEMAND:
			imageManipulator.handleResize(binaryField.getBinary(), ImageManipulator.applyDefaultManipulation(imageParams, binaryField))
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
			break;
		case MANUAL:
			HibBinary binary = binaryField.getBinary();
			HibImageVariant variant = imageVariantDao.getVariant(binary, imageParams, new InternalRoutingActionContextImpl(rc));
			if (variant == null) {
				throw error(NOT_FOUND, "node_error_binary_data_not_found");
			}
			respond(rc, variant, binaryField.getFileName(), binaryField.getMimeType());
			break;
		}
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

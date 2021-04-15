package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.storage.S3BinaryStorage;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.EncodeUtil;
import com.gentics.mesh.util.MimeTypeUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.ByteArrayInputStream;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MimeTypeUtils.DEFAULT_BINARY_MIME_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static java.util.Objects.isNull;

/**
 * Handler which will accept {@link S3BinaryGraphField} elements and return the binary data using the given context.
 */
@Singleton
public class S3BinaryFieldResponseHandler {

	private final ImageManipulator imageManipulator;

	private final S3BinaryStorage s3Binarystorage;

	private final S3Options s3Options;

	private final Vertx rxVertx;

	private final RangeRequestHandler rangeRequestHandler;

	@Inject
	public S3BinaryFieldResponseHandler(ImageManipulator imageManipulator,S3BinaryStorage s3Binarystorage, Vertx rxVertx, RangeRequestHandler rangeRequestHandler, MeshOptions meshOptions) {
		this.imageManipulator = imageManipulator;
		this.s3Binarystorage = s3Binarystorage;
		this.rxVertx = rxVertx;
		this.rangeRequestHandler = rangeRequestHandler;
		this.s3Options = meshOptions.getS3Options();
	}

	/**
	 * Handle the binary field response.
	 *
	 * @param rc
	 * @param s3binaryField
	 */
	public void handle(RoutingContext rc, S3BinaryGraphField s3binaryField,String uuid, String fieldName) {
		//rc.response().putHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		ImageManipulationParameters imageParams = ac.getImageParameters();
		boolean b = s3binaryField.hasProcessableImage();
		boolean b1 = imageParams.hasResizeParams();
		if (true&& imageParams.hasResizeParams()) {
			resizeAndRespond(rc, s3binaryField, imageParams);
		} else {
			respond(rc, s3binaryField);
		}
	}

	private boolean checkETag(RoutingContext rc, S3BinaryGraphField s3binaryField) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		String sha512sum = s3binaryField.getS3Binary().getSHA512Sum();
		String etagKey = sha512sum;
		if (s3binaryField.hasProcessableImage()) {
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

	private void respond(RoutingContext rc, S3BinaryGraphField s3binaryField) {
		String s3ObjectKey = s3binaryField.getS3Binary().getS3ObjectKey();
		s3Binarystorage.getPresignedUrl(s3Options.getBucket(), s3ObjectKey)
				.subscribe(model ->{
					rc.response().setStatusCode(302);
					rc.response().headers().set("Location", model.getPresignedUrl());
					rc.response().end();
				});
	}

	private void resizeAndRespond(RoutingContext rc, S3BinaryGraphField s3binaryField, ImageManipulationParameters imageParams) {
		HttpServerResponse response = rc.response();
		// We can maybe enhance the parameters using stored parameters.
		if (!imageParams.hasFocalPoint()) {
			FocalPoint fp = s3binaryField.getImageFocalPoint();
			if (fp != null) {
				imageParams.setFocalPoint(fp);
			}
		}
		Integer originalHeight = s3binaryField.getS3Binary().getImageHeight();
		Integer originalWidth = s3binaryField.getS3Binary().getImageWidth();

		if ("auto".equals(imageParams.getHeight())) {
			imageParams.setHeight(originalHeight);
		}
		if ("auto".equals(imageParams.getWidth())) {
			imageParams.setWidth(originalWidth);
		}
		String s3ObjectKey = s3binaryField.getS3Binary().getS3ObjectKey();
		String cacheS3ObjectKey = s3ObjectKey + "/image-" + imageParams.getCacheKey();
		String fileName = s3binaryField.getS3Binary().getFileName();
		imageManipulator
				.handleS3Resize(s3Options.getS3CacheOptions().getBucket(), cacheS3ObjectKey, fileName, imageParams)
				.andThen(Single.defer(() -> {
					Single<S3RestResponse> presignedUrl = s3Binarystorage.getPresignedUrl(s3Options.getS3CacheOptions().getBucket(), cacheS3ObjectKey);
					return presignedUrl;
				}))
				.doOnSuccess(model -> {
					rc.response().setStatusCode(302);
					rc.response().headers().set("Location", model.getPresignedUrl());
					rc.response().end();
				})
				.subscribe(ignore -> {
				}, rc::fail);
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

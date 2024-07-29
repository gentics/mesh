package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.ImageManipulationParameters;

import io.reactivex.Single;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler which will accept {@link S3HibBinaryField} elements and return the s3 binary data using the given context.
 */
@Singleton
public class S3BinaryFieldResponseHandler {

	private final ImageManipulator imageManipulator;
	private final S3BinaryStorage s3Binarystorage;
	private final S3Options s3Options;
	private final HttpServerConfig httpServerConfig;

	@Inject
	public S3BinaryFieldResponseHandler(ImageManipulator imageManipulator,S3BinaryStorage s3Binarystorage, MeshOptions meshOptions) {
		this.imageManipulator = imageManipulator;
		this.s3Binarystorage = s3Binarystorage;
		this.s3Options = meshOptions.getS3Options();
		this.httpServerConfig = meshOptions.getHttpServerOptions();
	}

	/**
	 * Handle the S3 binary field response.
	 *
	 * @param rc
	 * @param node 
	 * @param s3binaryField
	 */
	public void handle(RoutingContext rc, HibNode node, S3HibBinaryField s3binaryField) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc, httpServerConfig);
		ImageManipulationParameters imageParams = ac.getImageParameters();

		rc.response().putHeader(MeshHeaders.WEBROOT_NODE_UUID, node.getUuid());

		if (s3binaryField.hasProcessableImage() && imageParams.hasResizeParams()) {
			resizeAndRespond(rc, s3binaryField, imageParams);
		} else {
			respond(rc, s3binaryField);
		}
	}

	/**
	 * Handle the S3 binary field response when the S3 file is already there.
	 *
	 * @param rc
	 * @param s3binaryField
	 */
	private void respond(RoutingContext rc, S3HibBinaryField s3binaryField) {
		String s3ObjectKey = s3binaryField.getBinary().getS3ObjectKey();
		s3Binarystorage.exists(s3Options.getBucket(), s3ObjectKey).flatMap(
				(res) -> {
					if (res) {
						return s3Binarystorage.createDownloadPresignedUrl(s3Options.getBucket(), s3ObjectKey, false);
					} else {
						throw error(NOT_FOUND, "error_aws_s3binaryfield_not_found_with_name", s3ObjectKey);
					}
				}
		).doOnSuccess(model -> {
			rc.response().setStatusCode(302);
			rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "s3binary");
			rc.response().headers().set("Location", model.getPresignedUrl());
			rc.response().end();
		}).subscribe(ignore -> {
		}, rc::fail);
	}
	/**
	 * Handle the S3 binary field response when the S3 file does not exist or should be resized/manipulated first.
	 *
	 * @param rc
	 * @param s3binaryField
	 * @param imageParams
	 */
	private void resizeAndRespond(RoutingContext rc, S3HibBinaryField s3binaryField, ImageManipulationParameters imageParams) {
		// We can maybe enhance the parameters using stored parameters.
		if (!imageParams.hasFocalPoint()) {
			FocalPoint fp = s3binaryField.getImageFocalPoint();
			if (fp != null) {
				imageParams.setFocalPoint(fp);
			}
		}
		Integer originalHeight = s3binaryField.getBinary().getImageHeight();
		Integer originalWidth = s3binaryField.getBinary().getImageWidth();

		if ("auto".equals(imageParams.getHeight())) {
			imageParams.setHeight(originalHeight);
		}
		if ("auto".equals(imageParams.getWidth())) {
			imageParams.setWidth(originalWidth);
		}
		String s3ObjectKey = s3binaryField.getBinary().getS3ObjectKey();
		String cacheS3ObjectKey = s3ObjectKey + "/image-" + imageParams.getCacheKey();
		String fileName = s3binaryField.getBinary().getFileName();
		imageManipulator
				.handleS3CacheResize(s3Options.getBucket(), s3Options.getS3CacheOptions().getBucket(), s3ObjectKey, cacheS3ObjectKey, fileName, imageParams)
				.andThen(Single.defer(() -> {
					Single<S3RestResponse> presignedUrl = s3Binarystorage.createDownloadPresignedUrl(s3Options.getS3CacheOptions().getBucket(), cacheS3ObjectKey, true);
					return presignedUrl;
				}))
				.doOnSuccess(model -> {
					rc.response().setStatusCode(302);
					rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "s3binary");
					rc.response().headers().set("Location", model.getPresignedUrl());
					rc.response().end();
				})
				.subscribe(ignore -> {
				}, rc::fail);
	}
}

package com.gentics.mesh.core.binary.impl;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NodeUtil;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

/**
 * Processor which extracts basic image information (e.g. size, DPI)
 */
@Singleton
public class BasicImageDataProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(BasicImageDataProcessor.class);

	private ImageManipulator imageManipulator;

	@Inject
	public BasicImageDataProcessor(ImageManipulator imageManipulator) {
		this.imageManipulator = imageManipulator;
	}

	@Override
	public boolean accepts(String contentType) {
		return NodeUtil.isProcessableImage(contentType);
	}

	@Override
	public Completable process(ActionContext ac, FileUpload upload, BinaryGraphField field) {
		Optional<ImageInfo> infoOpt = imageManipulator.readImageInfo(upload.uploadedFileName()).doOnSuccess(ii -> {
			ac.put("imageInfo", ii);
		}).map(Optional::of).onErrorReturn(e -> {
			if (log.isDebugEnabled()) {
				log.warn("Could not read image information from upload {" + upload.fileName() + "/" + upload.name() + "}", e);
			}
			// suppress error
			return Optional.empty();
		}).blockingGet();

		// We found image information so lets store it.
		if (infoOpt.isPresent()) {
			Binary binary = field.getBinary();
			ImageInfo imageInfo = infoOpt.get();
			binary.setImageHeight(imageInfo.getHeight());
			binary.setImageWidth(imageInfo.getWidth());
			field.setImageDominantColor(imageInfo.getDominantColor());
		}

		return Completable.complete();
	}

}

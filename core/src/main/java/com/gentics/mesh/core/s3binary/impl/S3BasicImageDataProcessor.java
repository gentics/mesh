package com.gentics.mesh.core.s3binary.impl;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.s3binary.S3BinaryDataProcessor;
import com.gentics.mesh.core.s3binary.S3BinaryDataProcessorContext;
import com.gentics.mesh.util.NodeUtil;

import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.FileUpload;

/**
 * Processor which extracts basic image information (e.g. size, DPI) fpr S3 binaries
 */
@Singleton
public class S3BasicImageDataProcessor implements S3BinaryDataProcessor {

	private static final Logger log = LoggerFactory.getLogger(S3BasicImageDataProcessor.class);

	private final ImageManipulator imageManipulator;

	@Inject
	public S3BasicImageDataProcessor(ImageManipulator imageManipulator) {
		this.imageManipulator = imageManipulator;
	}

	@Override
	public boolean accepts(String contentType) {
		return NodeUtil.isProcessableImage(contentType);
	}

	@Override
	public Maybe<Consumer<S3HibBinaryField>> process(S3BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		return imageManipulator.readImageInfo(upload.uploadedFileName()).map(info -> {
			Consumer<S3HibBinaryField> consumer = field -> {
				log.debug("Setting info to binary field " + S3BinaryStorage.makeObjectKey(ctx.getNodeUuid(), ctx.getFieldName(), ctx.getLanguageTag()) + " - " + info);
				field.setImageDominantColor(info.getDominantColor());
				S3HibBinary binary = field.getBinary();
				binary.setImageHeight(info.getHeight());
				binary.setImageWidth(info.getWidth());
			};
			return consumer;
		}).doOnError(e -> {
			log.warn("Could not read image information from upload {" + upload.fileName() + "/" + upload.name() + "}", e);
		}).toMaybe().onErrorComplete();

	}

}

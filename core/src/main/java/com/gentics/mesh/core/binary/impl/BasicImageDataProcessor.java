package com.gentics.mesh.core.binary.impl;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.util.NodeUtil;

import io.reactivex.Maybe;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

/**
 * Processor which extracts basic image information (e.g. size, DPI)
 */
@Singleton
public class BasicImageDataProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(BasicImageDataProcessor.class);

	private final ImageManipulator imageManipulator;

	@Inject
	public BasicImageDataProcessor(ImageManipulator imageManipulator) {
		this.imageManipulator = imageManipulator;
	}

	@Override
	public boolean accepts(String contentType) {
		return NodeUtil.isProcessableImage(contentType);
	}

	@Override
	public Maybe<Consumer<HibBinaryField>> process(BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		return imageManipulator.readImageInfo(upload.uploadedFileName()).map(info -> {
			Consumer<HibBinaryField> consumer = field -> {
				log.info("Setting info to binary field " + field.getFieldKey() + " - " + info);
				field.setImageDominantColor(info.getDominantColor());
				HibBinary binary = field.getBinary();
				binary.setImageHeight(info.getHeight());
				binary.setImageWidth(info.getWidth());
			};
			return consumer;
		}).doOnError(e -> {
			if (log.isDebugEnabled()) {
				log.warn("Could not read image information from upload {" + upload.fileName() + "/" + upload.name() + "}", e);
			}
		}).toMaybe().onErrorComplete();

	}

}

package com.gentics.mesh.core.binary.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.graphdb.spi.Database;
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

	private final ImageManipulator imageManipulator;

	private final Database db;

	@Inject
	public BasicImageDataProcessor(Database db, ImageManipulator imageManipulator) {
		this.db = db;
		this.imageManipulator = imageManipulator;
	}

	@Override
	public boolean accepts(String contentType) {
		return NodeUtil.isProcessableImage(contentType);
	}

	@Override
	public Completable process(FileUpload upload, BinaryGraphField field) {
		return imageManipulator.readImageInfo(upload.uploadedFileName()).doOnSuccess(info -> {
			db.tx(() -> {
				log.info("Setting info to binary field " + field.getUuid() + " - " + info);
				field.setImageDominantColor(info.getDominantColor());
				Binary binary = field.getBinary();
				binary.setImageHeight(info.getHeight());
				binary.setImageWidth(info.getWidth());
			});
		}).toCompletable().doOnError(e -> {
			if (log.isDebugEnabled()) {
				log.warn("Could not read image information from upload {" + upload.fileName() + "/" + upload.name() + "}", e);
			}
		}).onErrorComplete();

	}

}

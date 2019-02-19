package com.gentics.mesh.core.binary.impl;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.image.spi.ImageInfo;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.util.NodeUtil;

import io.reactivex.Single;
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
	public void process(FileUpload upload, BinaryGraphField field) {
		imageManipulator.readImageInfo(upload.uploadedFileName()).map(Optional::of)
			.onErrorResumeNext(e -> {
				if (log.isDebugEnabled()) {
					log.warn("Could not read image information from upload {" + upload.fileName() + "/" + upload.name() + "}", e);
				}
				return Single.just(Optional.empty());
			}).subscribe(infoOpt -> {
				if (infoOpt.isPresent()) {
					DB.get().tx(() -> {
						ImageInfo info = infoOpt.get();
						Binary binary = field.getBinary();
						binary.setImageHeight(info.getHeight());
						binary.setImageWidth(info.getWidth());
						field.setImageDominantColor(info.getDominantColor());
					});
				}
			});

	}

}

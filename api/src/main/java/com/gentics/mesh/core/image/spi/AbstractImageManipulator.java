package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.query.impl.ImageManipulationParameter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

/**
 * Abstract image manipulator implementation.
 */
public abstract class AbstractImageManipulator implements ImageManipulator {

	private static final Logger log = LoggerFactory.getLogger(AbstractImageManipulator.class);

	protected ImageManipulatorOptions options;

	public AbstractImageManipulator(ImageManipulatorOptions options) {
		this.options = options;
	}

	
	@Override
	public Observable<Buffer> handleResize(File binaryFile, String sha512sum, ImageManipulationParameter parameters) {
		try {
		parameters.validate();
		parameters.validateLimits(options);
		} catch(Exception e) {
			return Observable.error(e);
		}
		try {
			InputStream ins = new FileInputStream(binaryFile);
			return handleResize(ins, sha512sum, parameters);
		} catch (FileNotFoundException e) {
			log.error("Can't handle image. File can't be opened. {" + binaryFile.getAbsolutePath() + "}", e);
			return Observable.error(error(BAD_REQUEST, "image_error_reading_failed", e));
		}
	}

	@Override
	public File getCacheFile(String sha512sum, ImageManipulationParameter parameters) {

		String[] parts = sha512sum.split("(?<=\\G.{8})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}

		File baseFolder = new File(options.getImageCacheDirectory(), buffer.toString());
		if (!baseFolder.exists()) {
			baseFolder.mkdirs();
		}
		File cacheFile = new File(baseFolder, "image-" + parameters.getCacheKey() + ".jpg");
		if (log.isDebugEnabled()) {
			log.debug("Using cache file {" + cacheFile + "}");
		}
		return cacheFile;
	}

}

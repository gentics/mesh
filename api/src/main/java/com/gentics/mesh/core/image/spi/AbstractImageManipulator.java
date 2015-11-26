package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

public abstract class AbstractImageManipulator implements ImageManipulator {

	private static final Logger log = LoggerFactory.getLogger(AbstractImageManipulator.class);

	@Override
	public Observable<Buffer> handleResize(File binaryFile, String sha512sum, ImageRequestParameter parameters) {
		parameters.validate();
		
		try {
			InputStream ins = new FileInputStream(binaryFile);
			return handleResize(ins, sha512sum, parameters);
		} catch (FileNotFoundException e) {
			// TODO i18n
			throw error(BAD_REQUEST, "Can't handle image. File can't be opened. {" + binaryFile.getAbsolutePath() + "}", e);
		}
	}

	@Override
	public File getCacheFile(String sha512sum, ImageRequestParameter parameters) {

		String[] parts = sha512sum.split("(?<=\\G.{8})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		String imageCacheDirectoryPath = Mesh.mesh().getOptions().getUploadOptions().getImageCacheDirectory();
		File baseFolder = new File(imageCacheDirectoryPath, buffer.toString());
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

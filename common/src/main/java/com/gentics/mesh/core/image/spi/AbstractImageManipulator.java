package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.gentics.mesh.core.image.CacheFileInfo;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.image.ImageManipulation;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Abstract image manipulator implementation.
 */
public abstract class AbstractImageManipulator implements ImageManipulator {

	private static final Logger log = LoggerFactory.getLogger(AbstractImageManipulator.class);

	protected ImageManipulatorOptions options;

	protected Vertx vertx;

	public AbstractImageManipulator(Vertx vertx, ImageManipulatorOptions options) {
		this.vertx = vertx;
		this.options = options;
	}

	@Override
	public Single<CacheFileInfo> getCacheFilePath(String sha512sum, ImageManipulation parameters) {
		ImageManipulationMode mode = options.getMode();

		switch (mode) {
		case OFF:
			throw error(BAD_REQUEST, "image_error_turned_off");
		case MANUAL:
		case ON_DEMAND:
			break;
		}

		FileSystem fs = vertx.fileSystem();

		String[] parts = sha512sum.split("(?<=\\G.{8})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}

		String baseFolder = Paths.get(options.getImageCacheDirectory(), buffer.toString()).toString();
		String baseName = "image-" + parameters.getCacheKey();

		return fs.rxMkdirs(baseFolder)
		// Vert.x uses Files.createDirectories internally, which will not fail when the folder already exists.
		// See https://github.com/eclipse-vertx/vert.x/issues/3029
		.andThen(fs.rxReadDir(baseFolder, baseName + "(\\..*)?"))
		.map(foundFiles -> {
			int numFiles = foundFiles.size();
			if (numFiles == 0) {
				String retPath = Paths.get(baseFolder, baseName).toString();
				if (log.isDebugEnabled()) {
					log.debug("No cache file found for base path {" + retPath + "}");
				}
				return new CacheFileInfo(retPath, false);
			}

			if (numFiles > 1) {
				String indent = System.lineSeparator() + "    - ";

				log.warn(
					"More than one cache file found:"
						+ System.lineSeparator() + "  hash: " + sha512sum
						+ System.lineSeparator() + "  key: " + parameters.getCacheKey()
						+ System.lineSeparator() + "  files:"
						+ indent
						+ String.join(indent, foundFiles)
						+ System.lineSeparator()
						+ "The cache directory {" + options.getImageCacheDirectory() + "} should be cleared");
			}

			if (log.isDebugEnabled()) {
				log.debug("Using cache file {" + foundFiles.size() + "}");
			}
			return new CacheFileInfo(foundFiles.get(0), true);
		});
	}

	@Override
	public Single<ImageInfo> readImageInfo(String path) {
		Maybe<ImageInfo> result = vertx.rxExecuteBlocking(bh -> {
			if (log.isDebugEnabled()) {
				log.debug("Reading image information from stream");
			}
			try {
				File file = new File(path);
				if (!file.exists()) {
					log.error("The image file {" + file.getAbsolutePath() + "} could not be found.");
					bh.fail(error(BAD_REQUEST, "image_error_reading_failed"));
					return;
				}
				BufferedImage image = ImageIO.read(file);
				if (image == null) {
					bh.fail(error(BAD_REQUEST, "image_error_reading_failed"));
				} else {
					bh.complete(toImageInfo(image));
				}
			} catch (Exception e) {
				log.error("Reading image information failed", e);
				bh.fail(e);
			}
		}, false);
		return result.toSingle();
	}

	/**
	 * Extract the image information from the given buffered image.
	 * 
	 * @param bi
	 * @return
	 */
	private ImageInfo toImageInfo(BufferedImage bi) {
		ImageInfo info = new ImageInfo();
		info.setWidth(bi.getWidth());
		info.setHeight(bi.getHeight());
		int[] rgb = calculateDominantColor(bi);
		// By default we assume white for the images
		String colorHex = "#FFFFFF";
		if (rgb.length >= 3) {
			colorHex = "#" + Integer.toHexString(rgb[0]) + Integer.toHexString(rgb[1]) + Integer.toHexString(rgb[2]);
		}
		info.setDominantColor(colorHex);
		return info;
	}

}

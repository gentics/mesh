package com.gentics.mesh.core.image.spi;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
	public File getCacheFile(String sha512sum, ImageManipulationParameters parameters) {

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

		String baseName = "image-" + parameters.getCacheKey();
		File[] foundFiles = baseFolder.listFiles((dir, name) -> name.startsWith(baseName));
		int numFiles = foundFiles.length;

		if (numFiles == 0) {
			File ret = new File(baseFolder, baseName);

			if (log.isDebugEnabled()) {
				log.debug("No cache file found for base path {" + ret.getAbsolutePath() + "}");
			}

			return ret;
		}

		if (numFiles > 1) {
			String indent = System.lineSeparator() + "    - ";

			log.warn(
				"More than one cache file found:"
					+ System.lineSeparator() + "  hash: " + sha512sum
					+ System.lineSeparator() + "  key: " + parameters.getCacheKey()
					+ System.lineSeparator() + "  files:"
					+ indent
					+ Arrays.stream(foundFiles).map(File::getName).collect(Collectors.joining(indent))
					+ System.lineSeparator()
					+ "The cache directory {" + options.getImageCacheDirectory() + "} should be cleared");
		}

		if (log.isDebugEnabled()) {
			log.debug("Using cache file {" + foundFiles[0] + "}");
		}

		return foundFiles[0];
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
		});
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

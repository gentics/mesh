package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

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
		File cacheFile = new File(baseFolder, "image-" + parameters.getCacheKey());
		if (log.isDebugEnabled()) {
			log.debug("Using cache file {" + cacheFile + "}");
		}
		return cacheFile;
	}

	@Override
	public Single<ImageInfo> readImageInfo(String file) {
		return vertx.rxExecuteBlocking(bh -> {
			if (log.isDebugEnabled()) {
				log.debug("Reading image information from stream");
			}
			try {
				BufferedImage image = ImageIO.read(new File(file));
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

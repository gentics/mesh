package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import rx.Single;
import rx.functions.Func0;

/**
 * Abstract image manipulator implementation.
 */
public abstract class AbstractImageManipulator implements ImageManipulator {

	private static final Logger log = LoggerFactory.getLogger(AbstractImageManipulator.class);

	protected ImageManipulatorOptions options;

	public AbstractImageManipulator(ImageManipulatorOptions options) {
		this.options = options;
	}

	// @Override
	// public Single<PropReadFileStream> handleResize(ReadStream<Buffer> stream, String sha512sum, ImageManipulationParameters parameters) {
	// try {
	// parameters.validate();
	// parameters.validateLimits(options);
	// } catch (Exception e) {
	// return Single.error(e);
	// }
	// return handleResize(stream, sha512sum, parameters);
	// }

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
		File cacheFile = new File(baseFolder, "image-" + parameters.getCacheKey() + ".jpg");
		if (log.isDebugEnabled()) {
			log.debug("Using cache file {" + cacheFile + "}");
		}
		return cacheFile;
	}

	@Override
	public Single<ImageInfo> readImageInfo(Func0<ReadStream<Buffer>> insFunc) {
		return Single.create(sub -> {
			// 1. Read the image
			ImageInfo info = new ImageInfo();
			Single<Buffer> data = RxUtil.readEntireData(insFunc.call());
			try (InputStream ins = new ByteArrayInputStream(data.toBlocking().value().getBytes())) {
				BufferedImage bi = ImageIO.read(ins);
				if (bi == null) {
					throw error(BAD_REQUEST, "image_error_reading_failed");
				}
				info.setWidth(bi.getWidth());
				info.setHeight(bi.getHeight());
				int[] rgb = calculateDominantColor(bi);
				// By default we assume white for the images
				String colorHex = "#FFFFFF";
				if (rgb.length >= 3) {
					colorHex = "#" + Integer.toHexString(rgb[0]) + Integer.toHexString(rgb[1]) + Integer.toHexString(rgb[2]);
				}
				info.setDominantColor(colorHex);
			} catch (Exception e) {
				throw error(BAD_REQUEST, "image_error_reading_failed", e);
			}

			// TODO The colorthief implementation which selectively samples the image is about 30% faster and will be even faster for bigger images
			// CMap result = ColorThief.getColorMap(bi, 5);
			// VBox vbox = result.vboxes.get(0);
			// int[] rgb = vbox.avg(false);

			sub.onSuccess(info);
		});

	}

}

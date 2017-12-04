package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.imageio.ImageIO;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Single;

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
	public Single<ImageInfo> readImageInfo(Observable<Buffer> stream) {
		return Single.create(sub -> {
			try (PipedInputStream pis = new PipedInputStream()) {
				PipedOutputStream pos = new PipedOutputStream(pis);
				stream.map(Buffer::getBytes).subscribeOn(RxHelper.blockingScheduler(Mesh.vertx())).doOnCompleted(() -> {
					try {
						pos.close();
					} catch (IOException e) {
						sub.onError(e);
					}
				}).subscribe(buf -> {
					try {
						pos.write(buf);
					} catch (IOException e) {
						sub.onError(e);
					}
				});

				BufferedImage bi = ImageIO.read(pis);
				if (bi == null) {
					throw error(BAD_REQUEST, "image_error_reading_failed");
				}
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
				sub.onSuccess(info);
			} catch (IOException e1) {
				sub.onError(e1);
			}
		});
	}

	@Override
	public Single<ImageInfo> readImageInfo(ReadStream<Buffer> stream) {
		return Single.create(sub -> {
			// 1. Read the image
			ImageInfo info = new ImageInfo();
			Single<Buffer> data = RxUtil.readEntireData(stream);
			// Single<Buffer> data = Single.just(Buffer.buffer());
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

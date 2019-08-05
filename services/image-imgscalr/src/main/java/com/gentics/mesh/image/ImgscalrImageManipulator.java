package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.image.spi.AbstractImageManipulator;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.graphdb.spi.Supplier;
import com.gentics.mesh.image.focalpoint.FocalPointModifier;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;
import com.twelvemonkeys.image.ResampleOp;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.WorkerExecutor;

/**
 * The ImgScalr Manipulator uses a pure java imageio image resizer.
 */
public class ImgscalrImageManipulator extends AbstractImageManipulator {

	private static final Logger log = LoggerFactory.getLogger(ImgscalrImageManipulator.class);

	private FocalPointModifier focalPointModifier;

	private WorkerExecutor workerPool;

	public ImgscalrImageManipulator() {
		this(new Vertx(Mesh.vertx()), Mesh.mesh().getOptions().getImageOptions());
	}

	ImgscalrImageManipulator(Vertx vertx, ImageManipulatorOptions options) {
		super(vertx, options);
		focalPointModifier = new FocalPointModifier(options);
		// 10 seconds
		workerPool = vertx.createSharedWorkerExecutor("resizeWorker", 5, Duration.ofSeconds(10).toNanos());
	}

	/**
	 * Crop the image if the request contains cropping parameters. Fail if the crop parameters are invalid or incomplete.
	 *
	 * @param originalImage
	 * @param cropArea
	 * @return cropped image or return original image if no cropping is requested
	 */
	protected BufferedImage crop(BufferedImage originalImage, ImageRect cropArea) {
		if (cropArea != null) {
			cropArea.validateCropBounds(originalImage.getWidth(), originalImage.getHeight());
			try {
				BufferedImage image = Scalr.crop(originalImage, cropArea.getStartX(), cropArea.getStartY(), cropArea.getWidth(),
					cropArea.getHeight());
				originalImage.flush();
				return image;
			} catch (IllegalArgumentException e) {
				throw error(BAD_REQUEST, "image_error_cropping_failed", e);
			}
		}
		return originalImage;
	}

	/**
	 * Resize the image if the request contains resize parameters.
	 *
	 * @param originalImage
	 * @param parameters
	 * @return Resized image or original image if no resize operation was requested
	 */
	protected BufferedImage resizeIfRequested(BufferedImage originalImage, ImageManipulationParameters parameters) {
		int originalHeight = originalImage.getHeight();
		int originalWidth = originalImage.getWidth();
		double aspectRatio = (double) originalWidth / (double) originalHeight;

		// Resize if required and calculate missing parameters if needed
		Integer pHeight = parameters.getHeight();
		Integer pWidth = parameters.getWidth();

		// Resizing is only needed when one of the parameters has been specified
		if (pHeight != null || pWidth != null) {

			// No operation needed when width is the same and no height was set
			if (pHeight == null && pWidth == originalWidth) {
				return originalImage;
			}

			// No operation needed when height is the same and no width was set
			if (pWidth == null && pHeight == originalHeight) {
				return originalImage;
			}

			// No operation needed when width and height match original image
			if (pWidth != null && pWidth == originalWidth && pHeight != null && pHeight == originalHeight) {
				return originalImage;
			}

			int width = pWidth == null ? (int) (pHeight * aspectRatio) : pWidth;
			int height = pHeight == null ? (int) (width / aspectRatio) : pHeight;
			try {
				BufferedImage image = Scalr.apply(originalImage, new ResampleOp(width, height, options.getResampleFilter().getFilter()));
				originalImage.flush();
				return image;
			} catch (IllegalArgumentException e) {
				throw error(BAD_REQUEST, "image_error_resizing_failed", e);
			}
		}

		return originalImage;
	}

	/**
	 * Create an image reader for the given input.
	 *
	 * @param input The input stream to read the original image from.
	 * @return An image reader reading from the given input stream
	 */
	private ImageReader getImageReader(ImageInputStream input) {
		Iterator<ImageReader> it = ImageIO.getImageReaders(input);

		if (!it.hasNext()) {
			// No reader available for this image type.
			log.error("No suitable image reader found for input image");

			throw error(BAD_REQUEST, "image_error_reading_failed");
		}

		ImageReader reader = it.next();

		reader.setInput(input, true);

		return reader;
	}

	/**
	 * Create an image writer from the same image format as the specified image reader writing
	 * to the given output stream.
	 *
	 * When no respective writer to the given reader is available, a PNG writer will be created.
	 *
	 * @param reader The reader used to read the original image
	 * @param out The output stream the writer should use
	 * @return An image writer for the same type as the specified reader, or a PNG writer if that is not available
	 */
	private ImageWriter getImageWriter(ImageReader reader, ImageOutputStream out) {
		ImageWriter writer = ImageIO.getImageWriter(reader);

		if (writer == null) {
			// This would mean we have a reader but no writer plugin available for the image type, which is highly unlikely, but just to be sure.
			log.debug("No suitable writer found for input image type");

			Iterator<ImageWriter> pngWriters = ImageIO.getImageWritersByFormatName("png");

			if (pngWriters.hasNext()) {
				log.debug("Trying to fall back to PNG");

				writer = pngWriters.next();
			}
		}

		if (writer == null) {
			throw error(BAD_REQUEST, "image_error_writing_failed");
		}

		writer.setOutput(out);

		if (log.isDebugEnabled()) {
			log.debug("Using writer " + writer.getClass().getName() + " for output");
		}

		ImageWriteParam params = writer.getDefaultWriteParam();

		if (params.canWriteProgressive()) {
			// TODO Maybe make this configurable or read from metadata of original image.
			// Note that it depends on the writer plugin used, if this setting is actually used.
			params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
		}

		// TODO Maybe make compression configurable or read from metadata of original image.

		return writer;
	}

	/**
	 * Resize the given image with the specified manipulation parameters.
	 *
	 * @param image The image to process
	 * @param parameters The parameters defining cropping and resizing requests
	 * @return The modified image
	 */
	private BufferedImage cropAndResize(BufferedImage image, ImageManipulationParameters parameters) {
		CropMode cropMode = parameters.getCropMode();
		boolean omitResize = false;
		if (cropMode != null) {
			switch (cropMode) {
				case RECT:
					image = crop(image, parameters.getRect());
					break;
				case FOCALPOINT:
					image = focalPointModifier.apply(image, parameters);
					// We don't need to resize the image again. The dimensions already match up with the target dimension
					omitResize = true;
					break;
			}
		}

		if (!omitResize) {
			image = resizeIfRequested(image, parameters);
		}

		return image;
	}

	@Override
	public Single<String> handleResize(Binary binary, ImageManipulationParameters parameters) {
		// Validate the resize parameters
		parameters.validate();
		parameters.validateLimits(options);

		Supplier<InputStream> stream = binary.openBlockingStream();

		return getCacheFilePath(binary.getSHA512Sum(), parameters)
			.flatMap(cacheFileInfo -> {
				if (cacheFileInfo.exists) {
					return Single.just(cacheFileInfo.path);
				} else {
					// TODO handle execution timeout
					// Make sure to run that code in the dedicated thread pool it may be CPU intensive for larger images and we don't want to exhaust the regular worker
					// pool
					return workerPool.<String>rxExecuteBlocking(bh -> {
						try (
							InputStream is = stream.get();
							ImageInputStream ins = ImageIO.createImageInputStream(is)
						) {
							BufferedImage image;
							ImageReader reader = getImageReader(ins);

							try {
								image = reader.read(0);
							} catch (IOException e) {
								log.error("Could not read input image", e);

								throw error(BAD_REQUEST, "image_error_reading_failed");
							}

							if (log.isDebugEnabled()) {
								log.debug("Read image from stream " + ins.hashCode() + " with reader " + reader.getClass().getName());
							}

							image = cropAndResize(image, parameters);

							String[] extensions = reader.getOriginatingProvider().getFileSuffixes();
							String extension = ArrayUtils.isEmpty(extensions) ? "" : extensions[0];
							String cacheFilePath = cacheFileInfo.path + "." + extension;
							File outCacheFile = new File(cacheFilePath);

							// Write image
							try (ImageOutputStream out = new FileImageOutputStream(outCacheFile)) {
								ImageWriteParam params = getImageWriteparams(extension);

								// same as write(image), but with image parameters
								getImageWriter(reader, out).write(null, new IIOImage(image, null, null), params);
							} catch (Exception e) {
								throw error(BAD_REQUEST, "image_error_writing_failed");
							}

							// Return buffer to written cache file
							bh.complete(cacheFilePath);
						} catch (Exception e) {
							bh.fail(e);
						}
					}, false).toSingle();
				}
			});
	}

	private ImageWriteParam getImageWriteparams(String extension) {
		if (isJpeg(extension)) {
			JPEGImageWriteParam params = new JPEGImageWriteParam(null);
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(options.getJpegQuality());
			return params;
		} else {
			return null;
		}
	}

	private boolean isJpeg(String extension) {
		extension = extension.toLowerCase();
		return extension.endsWith("jpg") || extension.endsWith("jpeg");
	}

	@Override
	public int[] calculateDominantColor(BufferedImage image) {
		// Resize the image to 1x1 and sample the pixel
		BufferedImage pixel = Scalr.resize(image, Mode.FIT_EXACT, 1, 1);
		image.flush();
		return pixel.getData().getPixel(0, 0, (int[]) null);
	}

	@Override
	public Single<Map<String, String>> getMetadata(InputStream ins) {
		return Single.create(sub -> {
			Parser parser = new AutoDetectParser();
			BodyContentHandler handler = new BodyContentHandler();
			Metadata metadata = new Metadata();
			ParseContext context = new ParseContext();
			try {
				parser.parse(ins, handler, metadata, context);
				Map<String, String> map = new HashMap<>();
				String[] metadataNames = metadata.names();

				for (String name : metadataNames) {
					map.put(name, metadata.get(name));
				}

				sub.onSuccess(map);

			} catch (Exception e) {
				sub.onError(e);
			}
			// ins.close();
		});
	}

}

package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.Objects.nonNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

import com.luciad.imageio.webp.WebPWriteParam;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.image.spi.AbstractImageManipulator;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.image.focalpoint.FocalPointModifier;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.util.NumberUtils;
import com.twelvemonkeys.image.ResampleOp;

import io.reactivex.Completable;
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

	private BinaryStorage binaryStorage;

	private S3BinaryStorage s3BinaryStorage;

	public ImgscalrImageManipulator(Vertx vertx, MeshOptions options, BinaryStorage binaryStorage, S3BinaryStorage s3BinaryStorage) {
		this(vertx, options.getImageOptions(), s3BinaryStorage);
		this.binaryStorage = binaryStorage;
	}

	ImgscalrImageManipulator(Vertx vertx, ImageManipulatorOptions options, S3BinaryStorage s3BinaryStorage) {
		super(vertx, options);
		focalPointModifier = new FocalPointModifier(options);
		// 10 seconds
		workerPool = vertx.createSharedWorkerExecutor("resizeWorker", 5, Duration.ofSeconds(10).toNanos());
		this.s3BinaryStorage = s3BinaryStorage;
	}

	/**
	 * Crop the image if the request contains cropping parameters. Fail if the crop
	 * parameters are invalid or incomplete.
	 *
	 * @param originalImage
	 * @param cropArea
	 * @return cropped image or return original image if no cropping is requested
	 */
	protected BufferedImage crop(BufferedImage originalImage, ImageRect cropArea) {
		if (cropArea != null) {
			cropArea.validateCropBounds(originalImage.getWidth(), originalImage.getHeight());
			try {
				BufferedImage image = Scalr.crop(originalImage, cropArea.getStartX(), cropArea.getStartY(),
						cropArea.getWidth(), cropArea.getHeight());
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
	protected BufferedImage resizeIfRequested(BufferedImage originalImage, ImageManipulation parameters) {
		int originalHeight = originalImage.getHeight();
		int originalWidth = originalImage.getWidth();
		double aspectRatio = (double) originalWidth / (double) originalHeight;

		// Resize if required and calculate missing parameters if needed
		Integer pHeight = NumberUtils.toInt(parameters.getHeight(), 0);
		Integer pWidth = NumberUtils.toInt(parameters.getWidth(), 0);

		// Resizing is only needed when one of the parameters has been specified
		if (pHeight != 0 || pWidth != 0) {

			// No operation needed when width is the same and no height was set
			if (pHeight == 0 && pWidth == originalWidth) {
				return originalImage;
			}

			// No operation needed when height is the same and no width was set
			if (pWidth == 0 && pHeight == originalHeight) {
				return originalImage;
			}

			// No operation needed when width and height match original image
			if (pWidth != 0 && pWidth == originalWidth && pHeight != 0 && pHeight == originalHeight) {
				return originalImage;
			}
			ResizeMode resizeMode = parameters.getResizeMode();
			// if the mode used is smart, and one of the dimensions is auto then set this
			// dimension to the original Value
			if (resizeMode == ResizeMode.SMART) {
				if (parameters.getWidth() != null && parameters.getWidth().equals("auto")) {
					pWidth = originalWidth;
				}
				if (parameters.getHeight() != null && parameters.getHeight().equals("auto")) {
					pHeight = originalHeight;
				}
			}
			int width = pWidth == 0 ? (int) (pHeight * aspectRatio) : pWidth;
			int height = pHeight == 0 ? (int) (width / aspectRatio) : pHeight;

			// if we want to use smart resizing we need to crop the original image to the
			// correct format before resizing to avoid distortion
			if (pWidth != 0 && pHeight != 0 && resizeMode == ResizeMode.SMART) {

				double pAspectRatio = (double) pWidth / (double) pHeight;
				if (aspectRatio != pAspectRatio) {
					if (aspectRatio < pAspectRatio) {
						// crop height (top & bottom)
						int resizeHeight = Math.max(1, (int) (originalWidth / pAspectRatio));
						int startY = (int) (originalHeight * 0.5 - resizeHeight * 0.5);
						originalImage = crop(originalImage, new ImageRect(0, startY, resizeHeight, originalWidth));
					} else {
						// crop width (left & right)
						int resizeWidth = Math.max(1, (int) (originalHeight * pAspectRatio));
						int startX = (int) (originalWidth * 0.5 - resizeWidth * 0.5);
						originalImage = crop(originalImage, new ImageRect(startX, 0, originalHeight, resizeWidth));
					}
				}
			}

			// if we want to use proportional resizing we need to make sure the destination
			// dimension fits inside the provided dimensions
			if (pWidth != 0 && pHeight != 0 && resizeMode == ResizeMode.PROP) {
				double pAspectRatio = (double) pWidth / (double) pHeight;
				if (aspectRatio < pAspectRatio) {
					// scale to pHeight
					width = Math.max(1, (int) (pHeight * aspectRatio));
					height = Math.max(1, pHeight);
				} else {
					// scale to pWidth
					width = Math.max(1, pWidth);
					height = Math.max(1, (int) (pWidth / aspectRatio));
				}

				// Should the resulting format be the same as the original image we do not need
				// to resize
				if (width == originalWidth && height == originalHeight) {
					return originalImage;
				}
			}

			try {
				BufferedImage image = Scalr.apply(originalImage,
						new ResampleOp(width, height, options.getResampleFilter().getFilter()));
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
	 * Create an image writer from the same image format as the specified image
	 * reader writing to the given output stream. When no respective writer to the
	 * given reader is available, a PNG writer will be created.
	 *
	 * @param reader The reader used to read the original image
	 * @param out    The output stream the writer should use
	 * @return An image writer for the same type as the specified reader, or a PNG
	 *         writer if that is not available
	 */
	private ImageWriter getImageWriter(ImageReader reader, ImageOutputStream out) {
		ImageWriter writer = ImageIO.getImageWriter(reader);

		if (writer == null) {
			// This would mean we have a reader but no writer plugin available for the image
			// type, which is highly unlikely, but just to be sure.
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
			// Note that it depends on the writer plugin used, if this setting is actually
			// used.
			params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
		}

		// TODO Maybe make compression configurable or read from metadata of original
		// image.

		return writer;
	}

	/**
	 * Resize the given image with the specified manipulation parameters.
	 *
	 * @param image      The image to process
	 * @param parameters The parameters defining cropping and resizing requests
	 * @return The modified image
	 */
	protected BufferedImage cropAndResize(BufferedImage image, ImageManipulation parameters) {
		CropMode cropMode = parameters.getCropMode();
		boolean omitResize = false;
		if (cropMode != null) {
			switch (cropMode) {
			case RECT:
				image = crop(image, parameters.getRect());
				break;
			case FOCALPOINT:
				image = focalPointModifier.apply(image, parameters);
				// We don't need to resize the image again. The dimensions already match up with
				// the target dimension
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
	public Single<String> handleResize(HibBinary binary, ImageManipulation parameters) {
		ImageManipulationMode mode = options.getMode();

		if (ImageManipulationMode.OFF == mode) {
			throw error(BAD_REQUEST, "image_error_reading_failed");
		}
		// Validate the resize parameters
		parameters.validateManipulation();
		parameters.validateLimits(options);

		String binaryUuid = binary.getUuid();

		return getCacheFilePath(binary.getSHA512Sum(), parameters).flatMap(cacheFileInfo -> {
			if (cacheFileInfo.exists) {
				return Single.just(cacheFileInfo.path);
			} else {
				// TODO handle execution timeout
				// Make sure to run that code in the dedicated thread pool it may be CPU
				// intensive for larger images and we don't want to exhaust the
				// regular worker
				// pool
				return workerPool.<String>rxExecuteBlocking(bh -> {
					Supplier<InputStream> stream = () -> binaryStorage.openBlockingStream(binaryUuid);

					try (InputStream is = stream.get(); ImageInputStream ins = ImageIO.createImageInputStream(is)) {
						BufferedImage image;
						ImageReader reader = getImageReader(ins);

						try {
							image = reader.read(0);
						} catch (IOException e) {
							log.error("Could not read input image", e);

							throw error(BAD_REQUEST, "image_error_reading_failed");
						}

						if (log.isDebugEnabled()) {
							log.debug("Read image from stream " + ins.hashCode() + " with reader "
									+ reader.getClass().getName());
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

	@Override
	public Single<File> handleS3Resize(String bucketName, String s3ObjectKey, String filename,
			ImageManipulationParameters parameters) {
		// Validate the resize parameters
		parameters.validateManipulation();
		parameters.validateLimits(options);

		return s3BinaryStorage.read(bucketName, s3ObjectKey)
				.flatMapSingle(originalFile -> workerPool.<File>rxExecuteBlocking(bh -> {
					try (InputStream is = new ByteArrayInputStream(originalFile.getBytes());
							ImageInputStream ins = ImageIO.createImageInputStream(is)) {
						BufferedImage image;
						ImageReader reader = getImageReader(ins);

						try {
							image = reader.read(0);
						} catch (IOException e) {
							log.error("Could not read input image", e);
							throw error(BAD_REQUEST, "image_error_reading_failed");
						}

						if (log.isDebugEnabled()) {
							log.debug("Read image from stream " + ins.hashCode() + " with reader "
									+ reader.getClass().getName());
						}

						image = cropAndResize(image, parameters);

						String[] extensions = reader.getOriginatingProvider().getFileSuffixes();
						String extension = ArrayUtils.isEmpty(extensions) ? "" : extensions[0];
						String cacheFilePath = options.getImageCacheDirectory()  + File.pathSeparator + filename;
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
						bh.complete(outCacheFile);
					} catch (Exception e) {
						bh.fail(e);
					}
				}).toSingle()).flatMapSingle(file ->
				// write cache to AWS
				s3BinaryStorage.uploadFile(bucketName, s3ObjectKey, file, true)
						.flatMap(ignoreElement -> Single.just(file)))
				.singleOrError();
	}

	@Override
	public Completable handleS3CacheResize(String bucketName, String cacheBucketName, String s3ObjectKey,
			String cacheS3ObjectKey, String filename, ImageManipulationParameters parameters) {
		// Validate the resize parameters
		parameters.validate();
		parameters.validateLimits(options);

		return s3BinaryStorage.exists(cacheBucketName, cacheS3ObjectKey)
				// read from aws and return buffer with data
				.flatMapCompletable(res -> {
					if (res)
						return s3BinaryStorage.read(cacheBucketName, cacheS3ObjectKey)
								.flatMapCompletable(cacheFileInfo -> {
									if (nonNull(cacheFileInfo) || cacheFileInfo.getBytes().length > 0) {
										return Completable.complete();
									} else {
										log.error("Could not read input image");
										return Completable
												.error(error(INTERNAL_SERVER_ERROR, "image_error_reading_failed"));
									}
								});
					else {
						return s3BinaryStorage.read(bucketName, s3ObjectKey)
								.flatMapSingle(originalFile -> workerPool.<File>rxExecuteBlocking(bh -> {
									try (InputStream is = new ByteArrayInputStream(originalFile.getBytes());
											ImageInputStream ins = ImageIO.createImageInputStream(is)) {
										BufferedImage image;
										ImageReader reader = getImageReader(ins);

										try {
											image = reader.read(0);
										} catch (IOException e) {
											log.error("Could not read input image", e);
											throw error(BAD_REQUEST, "image_error_reading_failed");
										}

										if (log.isDebugEnabled()) {
											log.debug("Read image from stream " + ins.hashCode() + " with reader "
													+ reader.getClass().getName());
										}

										image = cropAndResize(image, parameters);

										String[] extensions = reader.getOriginatingProvider().getFileSuffixes();
										String extension = ArrayUtils.isEmpty(extensions) ? "" : extensions[0];
										String cacheFilePath = options.getImageCacheDirectory()  + File.pathSeparator + filename;
										File outCacheFile = new File(cacheFilePath);

										// Write image
										try (ImageOutputStream out = new FileImageOutputStream(outCacheFile)) {
											ImageWriteParam params = getImageWriteparams(extension);

											// same as write(image), but with image parameters
											getImageWriter(reader, out).write(null, new IIOImage(image, null, null),
													params);
										} catch (Exception e) {
											throw error(BAD_REQUEST, "image_error_writing_failed");
										}
										// Return buffer to written cache file
										bh.complete(outCacheFile);
									} catch (Exception e) {
										bh.fail(e);
									}
								}).toSingle()).flatMapSingle(file ->
						// write cache to AWS
						s3BinaryStorage.uploadFile(cacheBucketName, cacheS3ObjectKey, file, true)).ignoreElements();
					}
				});
	}

	private ImageWriteParam getImageWriteparams(String extension) {
		if (isJpeg(extension)) {
			JPEGImageWriteParam params = new JPEGImageWriteParam(null);
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(options.getJpegQuality());
			return params;
		}

		if (isWebP(extension)) {
			WebPWriteParam params = (WebPWriteParam) ImageIO.getImageWritersByMIMEType("image/webp").next().getDefaultWriteParam();

			params.setCompressionType("Lossy");
			params.setCompressionQuality(options.getJpegQuality());

			return params;
		}

		return null;
	}

	private boolean isJpeg(String extension) {
		extension = extension.toLowerCase();
		return extension.endsWith("jpg") || extension.endsWith("jpeg");
	}

	private boolean isWebP(String extension) {
		return "webp".equalsIgnoreCase(extension);
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

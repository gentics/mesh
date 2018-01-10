package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.image.spi.AbstractImageManipulator;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.util.PropReadFileStream;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Vertx;

/**
 * The ImgScalr Manipulator uses a pure java imageio image resizer.
 */
public class ImgscalrImageManipulator extends AbstractImageManipulator {

	public ImgscalrImageManipulator() {
		this(new Vertx(Mesh.vertx()), Mesh.mesh().getOptions().getImageOptions());
	}

	ImgscalrImageManipulator(Vertx vertx, ImageManipulatorOptions options) {
		super(vertx, options);
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
				BufferedImage image = Scalr.crop(originalImage, cropArea.getStartX(), cropArea.getStartY(), cropArea.getWidth(), cropArea
						.getHeight());
				originalImage.flush();
				return image;
			} catch (IllegalArgumentException e) {
				throw error(BAD_REQUEST, "image_error_cropping_failed", e);
			}
		}
		return originalImage;
	}

	/**
	 * First resize the image and later crop the image to focus the focal point.
	 * 
	 * @param rgbCopy
	 * @param parameters
	 * @return resized and cropped image
	 */
	protected BufferedImage resizeAndCropFocalPoint(BufferedImage rgbCopy, ImageManipulationParameters parameters) {
		parameters.validate();
		Integer targetWidth = parameters.getWidth();
		Integer targetHeight = parameters.getHeight();
		// TODO validate the focal point against the source image dimensions

		// TODO load the stored focalpoint and omit it if a custom one has been provided
		parameters.getFocalPoint();

		// TODO resize the image to the largest dimension while keeping the aspect ratio

		// TODO next crop the other dimension using the focal point and choose the crop area closest to the middle of the needed focal point axis.
		return rgbCopy;
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
				BufferedImage image = Scalr.resize(originalImage, Mode.FIT_EXACT, width, height);
				originalImage.flush();
				return image;
			} catch (IllegalArgumentException e) {
				throw error(BAD_REQUEST, "image_error_resizing_failed", e);
			}
		}

		return originalImage;
	}

	@Override
	public Single<PropReadFileStream> handleResize(Observable<Buffer> stream, String cacheKey, ImageManipulationParameters parameters) {
		// Validate the resize parameters
		try {
			parameters.validate();
			parameters.validateLimits(options);
		} catch (Exception e) {
			return Single.error(e);
		}
		File cacheFile = getCacheFile(cacheKey, parameters);

		// Check the cache file directory
		if (cacheFile.exists()) {
			return PropReadFileStream.openFile(this.vertx, cacheFile.getAbsolutePath());
		}

		// Read the image and apply the changes - Make sure to run that code in a blocking scheduler since it may be CPU intensive for larger images.
		return readImage(stream).observeOn(RxHelper.blockingScheduler(vertx.getDelegate(), false)).flatMap(bi -> {
			if (bi == null) {
				throw error(BAD_REQUEST, "image_error_reading_failed");
			}
			if (bi.getTransparency() == Transparency.TRANSLUCENT) {
				// NOTE: For BITMASK images, the color model is likely IndexColorModel,
				// and this model will contain the "real" color of the transparent parts
				// which is likely a better fit than unconditionally setting it to white.
				// Fill background with white
				Graphics2D graphics = bi.createGraphics();
				try {
					graphics.setComposite(AlphaComposite.DstOver); // Set composite rules to paint "behind"
					graphics.setPaint(Color.WHITE);
					graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
				} finally {
					graphics.dispose();
				}
			}
			// Convert the image to RGB for images with transparency (gif, png)
			BufferedImage rgbCopy = bi;
			if (bi.getTransparency() == Transparency.TRANSLUCENT || bi.getTransparency() == Transparency.BITMASK) {
				rgbCopy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = rgbCopy.createGraphics();
				graphics.drawImage(bi, 0, 0, Color.WHITE, null);
				graphics.dispose();
			}

			// Manipulate image
			CropMode cropMode = parameters.getCropMode();
			boolean omitResize = false;
			if (cropMode != null) {
				switch (cropMode) {
				case RECT:
					rgbCopy = crop(rgbCopy, parameters.getRect());
					break;
				case FOCALPOINT:
					rgbCopy = resizeAndCropFocalPoint(rgbCopy, parameters);
					// We don't need to resize the image again. The dimensions already match up with the target dimension 
					omitResize = true;
					break;
				}
			}

			if (!omitResize) {
				rgbCopy = resizeIfRequested(rgbCopy, parameters);
			}

			// Write image
			try {
				ImageIO.write(rgbCopy, "jpg", cacheFile);
			} catch (Exception e) {
				throw error(BAD_REQUEST, "image_error_writing_failed", e);
			}
			// Return buffer to written cache file
			return PropReadFileStream.openFile(this.vertx, cacheFile.getAbsolutePath());
		});

	}

	/**
	 * Read the image data stream and return the decoded image.
	 * 
	 * @param stream
	 * @return
	 */
	private Single<BufferedImage> readImage(Observable<Buffer> stream) {
		try {
			InputStream ins = RxUtil.toInputStream(stream, vertx);
			return vertx.rxExecuteBlocking(bc -> {
				try {
					BufferedImage image = ImageIO.read(ins);
					ins.close();
					bc.complete(image);
				} catch (IOException e) {
					bc.fail(e);
				}
			}, false);
		} catch (IOException e1) {
			return Single.error(e1);
		}
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

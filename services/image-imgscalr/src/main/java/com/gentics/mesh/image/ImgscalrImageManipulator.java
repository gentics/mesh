package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.image.spi.AbstractImageManipulator;
import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

/**
 * The ImgScalr Manipulator uses a pure java imageio image resizer.
 */
public class ImgscalrImageManipulator extends AbstractImageManipulator {

	public ImgscalrImageManipulator() {
	}

	/**
	 * Crop the image if the request contains cropping parameters. Fail if the crop parameters are invalid or incomplete.
	 * 
	 * @param originalImage
	 * @param parameters
	 * @return cropped image or return original image if no cropping is requested
	 */
	protected BufferedImage cropIfRequested(BufferedImage originalImage, ImageRequestParameter parameters) {
		if (!parameters.hasValidOrNoneCropParameters()) {
			// TODO i18n
			throw error(BAD_REQUEST, "Not all crop parameters have been specified.");
		}

		if (parameters.hasAllCropParameters()) {
			try {
				BufferedImage image = Scalr.crop(originalImage, parameters.getStartx(), parameters.getStarty(), parameters.getCropw(),
						parameters.getCroph());
				originalImage.flush();
				return image;
			} catch (IllegalArgumentException e) {
				// TODO catch potential errors early and return a nice i18n error
				throw error(BAD_REQUEST, "Cropping failed.", e);
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
	protected BufferedImage resizeIfRequested(BufferedImage originalImage, ImageRequestParameter parameters) {
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
				// TODO catch potential errors early and return a nice i18n error
				throw error(BAD_REQUEST, "Resizing failed", e);
			}
		}

		return originalImage;
	}

	@Override
	public Observable<Buffer> handleResize(InputStream ins, String sha512sum, ImageRequestParameter parameters) {
		Vertx vertx = new Vertx(Mesh.vertx());
		File cacheFile = getCacheFile(sha512sum, parameters);

		// 1. Check the cache file directory
		if (cacheFile.exists()) {
			return vertx.fileSystem().readFileObservable(cacheFile.getAbsolutePath());
		}

		try {
			// 2. Read the image
			BufferedImage bi = ImageIO.read(ins);
			if (bi == null) {
				// TODO i18n
				throw error(BAD_REQUEST, "Can't handle image {" + sha512sum + "}");
			}

			// 3. Manipulate image
			bi = cropIfRequested(bi, parameters);
			bi = resizeIfRequested(bi, parameters);

			// 4. Write image
			ImageIO.write(bi, "jpg", cacheFile);
		} catch (Exception e) {
			// TODO i18n
			throw error(BAD_REQUEST, "Can't handle image {" + sha512sum + "}", e);
		}

		// 5. Return buffer to written cache file
		return vertx.fileSystem().readFileObservable(cacheFile.getAbsolutePath());
	}

}

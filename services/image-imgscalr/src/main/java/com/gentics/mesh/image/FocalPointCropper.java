package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;

import com.gentics.mesh.core.rest.node.field.Point;
import com.gentics.mesh.parameter.ImageManipulationParameters;

public class FocalPointCropper {

	/**
	 * First resize the image and later crop the image to focus the focal point.
	 * 
	 * @param rgbCopy
	 * @param parameters
	 * @return resized and cropped image
	 */
	protected BufferedImage apply(BufferedImage rgbCopy, ImageManipulationParameters parameters) {
		Point focalPoint = parameters.getFocalPoint();
		if (focalPoint == null) {
			return rgbCopy;
		}

		// Validate the focal point position
		Point imageSize = new Point(rgbCopy.getWidth(), rgbCopy.getHeight());
		if (!parameters.getFocalPoint().isWithinBoundsOf(imageSize)) {
			throw error(BAD_REQUEST, "image_error_focalpoint_out_of_bounds", focalPoint.toString(), imageSize.toString());
		}

		Integer targetWidth = parameters.getWidth();
		Integer targetHeight = parameters.getHeight();

		// Determine which image dimension is nearest to the target dimension
		int deltaX = Math.abs(targetWidth - imageSize.getX());
		int deltaY = Math.abs(targetHeight - imageSize.getY());
		double aspectRatio = imageSize.getRatio();
		// Delta on y is smaller thus we resize to that dimension
		boolean alignY = deltaX > deltaY;
		if (alignY) {

		}

		// TODO resize the image to the largest dimension while keeping the aspect ratio
		// try {
		// BufferedImage image = Scalr.resize(originalImage, Mode.FIT_EXACT, width, height);
		// originalImage.flush();
		// return image;
		// } catch (IllegalArgumentException e) {
		// throw error(BAD_REQUEST, "image_error_resizing_failed", e);
		// }

		// Next we need to crop the image in order to achieve the final targeted size
		// TODO next crop the other dimension using the focal point and choose the crop area closest to the middle of the needed focal point axis.
		int startX = 0;
		int startY = 0;
		if (alignY) {
			//focalPoint.getY()
			startY = 0;
		} else {
			//focalPoint.getX()
			startX = 0;
		}
		try {
			rgbCopy = Scalr.crop(rgbCopy, startX, startY, targetWidth, targetHeight);
			rgbCopy.flush();
		} catch (IllegalArgumentException e) {
			throw error(BAD_REQUEST, "image_error_cropping_failed", e);
		}

		return rgbCopy;
	}

}

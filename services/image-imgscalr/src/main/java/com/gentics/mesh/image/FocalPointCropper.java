package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.core.rest.node.field.Point;
import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Implementation of the focal point cropper. This cropper will: 1. Resize the image so that it exceeds the targeted size in one dimension 2. Crop the image in
 * a way so that the targeted size is reached
 */
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

		if (parameters.getFocalPointDebug()) {
			drawFocusPointAxis(rgbCopy, focalPoint);
		}

		// Validate the focal point position
		Point imageSize = new Point(rgbCopy.getWidth(), rgbCopy.getHeight());
		if (!parameters.getFocalPoint().isWithinBoundsOf(imageSize)) {
			throw error(BAD_REQUEST, "image_error_focalpoint_out_of_bounds", focalPoint.toString(), imageSize.toString());
		}

		Point targetSize = parameters.getSize();
		boolean alignX = targetSize.getX() > targetSize.getY();
		Point resize = calculateResize(imageSize, targetSize);

		// Resize the image to the largest dimension while keeping the aspect ratio
		try {
			rgbCopy = Scalr.resize(rgbCopy, Mode.FIT_EXACT, resize.getX(), resize.getY());
		} catch (IllegalArgumentException e) {
			throw error(BAD_REQUEST, "image_error_resizing_failed", e);
		}

		// Recalculate the focal point position since the image has been resized
		double fpxd = (double) focalPoint.getX() / (double) imageSize.getX();
		double fpyd = (double) focalPoint.getY() / (double) imageSize.getY();
		int fpx = (int) (fpxd * resize.getX());
		int fpy = (int) (fpyd * resize.getY());
		focalPoint = new Point(fpx, fpy);

		// Update the image size
		imageSize = resize;

		Point cropStart = calculateCropStart(alignX, targetSize, imageSize, focalPoint);

		try {
			rgbCopy = Scalr.crop(rgbCopy, cropStart.getX(), cropStart.getY(), targetSize.getX(), targetSize.getY());
			rgbCopy.flush();
		} catch (IllegalArgumentException e) {
			throw error(BAD_REQUEST, "image_error_cropping_failed", e);
		}

		return rgbCopy;
	}

	/**
	 * Calculate the start point of the crop area. The point will take the given focal point and the image size into account.
	 * 
	 * @param alignX
	 * @param targetSize
	 * @param imageSize
	 * @param focalPoint
	 * @return Calculated start point
	 */
	protected Point calculateCropStart(boolean alignX, Point targetSize, Point imageSize, Point focalPoint) {
		// Next we need to crop the image in order to achieve the final targeted size
		// TODO next crop the other dimension using the focal point and choose the crop area closest to the middle of the needed focal point axis.
		int startX = 0;
		int startY = 0;
		if (!alignX) {
			int half = targetSize.getX() / 2;
			startX = focalPoint.getX() - half;
			// Clamp the start point to zero if the start-y value would be outside of the image.
			startX = startX < 0 ? 0 : startX;
			// We may need to move the start point to the right
			if (startX - targetSize.getX() > 0) {
				startX = imageSize.getX() - targetSize.getY();
			}
		} else {
			int half = targetSize.getY() / 2;
			startY = focalPoint.getY() - half;
			// Clamp the start point to zero if the start-y value would be outside of the image.
			startY = startY < 0 ? 0 : startY;
			// We may need to move the point up. Otherwise the resulting crop area would be outside of the image bounds
			if (startY - targetSize.getY() > 0) {
				startY = imageSize.getY() - targetSize.getY();
			}
		}
		return new Point(startX, startY);
	}

	/**
	 * Draw the focal point axis in the image.
	 * 
	 * @param img
	 * @param focalPoint
	 */
	protected void drawFocusPointAxis(BufferedImage img, Point focalPoint) {
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(3));
		// x-axis of focal point
		g2.drawLine(focalPoint.getX(), 0, focalPoint.getX(), img.getHeight());
		// y-axis of focal point
		g2.drawLine(0, focalPoint.getY(), img.getWidth(), focalPoint.getY());
	}

	protected Point calculateResize(Point imageSize, Point targetSize) {
		Integer targetWidth = targetSize.getX();
		Integer targetHeight = targetSize.getY();

		// Determine which image dimension is nearest to the target dimension
		int deltaX = targetWidth - imageSize.getX();
		int deltaY = targetHeight - imageSize.getY();
		int resizeX = targetWidth;
		int resizeY = targetHeight;
		double aspectRatio = imageSize.getRatio();
		boolean alignX = targetWidth > targetHeight;
		if (alignX) {
			double c = Math.floor((double) targetWidth / aspectRatio);
			resizeY = (int) c;
		} else {
			double c = Math.floor((double) targetHeight * aspectRatio);
			resizeX = (int) c;
		}

		return new Point(resizeX, resizeY);

	}

}

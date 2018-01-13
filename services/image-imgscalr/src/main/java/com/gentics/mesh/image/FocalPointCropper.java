package com.gentics.mesh.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Implementation of the focal point cropper. This cropper will: 1. Resize the image so that it exceeds the targeted size in one dimension 2. Crop the image in
 * a way so that the targeted size is reached
 */
public class FocalPointCropper {

	/**
	 * First resize the image and later crop the image to focus the focal point.
	 * 
	 * @param img
	 * @param parameters
	 * @return resized and cropped image
	 */
	protected BufferedImage apply(BufferedImage img, ImageManipulationParameters parameters) {
		FocalPoint focalPoint = parameters.getFocalPoint();
		if (focalPoint == null) {
			return img;
		}

		if (parameters.getFocalPointDebug()) {
			drawFocusPointAxis(img, focalPoint);
		}

		// Validate the focal point position
		Point imageSize = new Point(img.getWidth(), img.getHeight());
		Point absFocalPoint = parameters.getFocalPoint().convertToAbsolutePoint(imageSize);
		if (!absFocalPoint.isWithinBoundsOf(imageSize)) {
			throw error(BAD_REQUEST, "image_error_focalpoint_out_of_bounds", focalPoint.toString(), imageSize.toString());
		}

		Point targetSize = parameters.getSize();
		Point newSize = calculateResize(imageSize, targetSize);

		// Resize the image to the largest dimension while keeping the aspect ratio
		img = applyResize(img, newSize);

		// No need for cropping. The image has already the target dimensions
		if (imageSize.equals(targetSize)) {
			return img;
		}

		boolean alignX = calculateAlignment(imageSize, targetSize);
		Point cropStart = calculateCropStart(alignX, targetSize, newSize, focalPoint);
		if (cropStart != null) {
			img = applyCrop(img, cropStart, targetSize);
		}

		// TODO Add focal point zoom handling. Zooming should happen before other operations in order to preserve image quality 
		// parameters.getFocalZoom();
		// Float zoomFactor = 2f;
		// img = applyZoom(img, zoomFactor, focalPoint);

		img.flush();
		return img;
	}

	/**
	 * Apply the given zoom by cropping and resizing the image back to the original size. It is only supported to zoom in. Zooming out is not possible.
	 * 
	 * @param img
	 * @param zoomFactor
	 *            Positive zoom factor. Negative values will not result in any changes to the image
	 * @param focalPoint
	 * @return
	 */
	private BufferedImage applyZoom(BufferedImage img, Float zoomFactor, FocalPoint focalPoint) {
		if (zoomFactor == null || zoomFactor <= 1) {
			return img;
		}
		int x = img.getWidth();
		int y = img.getHeight();

		AffineTransform af = new AffineTransform();
		af.scale(1 / zoomFactor, 1 / zoomFactor);
		// af.translate(x / focalPoint.getX(), y / focalPoint.getY());

		AffineTransformOp operation = new AffineTransformOp(af, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		img = operation.filter(img, null);

		// Now resize it back to the actual dimension
		img = applyResize(img, new Point(x, y));

		return img;
	}

	/**
	 * Resize the image.
	 * 
	 * @param img
	 * @param size
	 * @return
	 */
	private BufferedImage applyResize(BufferedImage img, Point size) {
		try {
			return Scalr.resize(img, Mode.FIT_EXACT, size.getX(), size.getY());
		} catch (IllegalArgumentException e) {
			throw error(BAD_REQUEST, "image_error_resizing_failed", e);
		}
	}

	/**
	 * Crop the image.
	 * 
	 * @param img
	 * @param cropStart
	 * @param cropSize
	 * @return
	 */
	private BufferedImage applyCrop(BufferedImage img, Point cropStart, Point cropSize) {
		try {
			return Scalr.crop(img, cropStart.getX(), cropStart.getY(), cropSize.getX(), cropSize.getY());
		} catch (IllegalArgumentException e) {
			throw error(BAD_REQUEST, "image_error_cropping_failed", e);
		}
	}

	/**
	 * Determine which dimension to resize.
	 * 
	 * @param imageSize
	 * @param targetSize
	 * @return
	 */
	protected boolean calculateAlignment(Point imageSize, Point targetSize) {
		double ratio = imageSize.getRatio();

		// Determine which resize operation would yield the largest image. We will crop the rest of the image and thus align the image by that dimension.
		int pixelByX = (int) (targetSize.getX() / ratio) * targetSize.getX();
		int pixelByY = (int) (targetSize.getY() * ratio) * targetSize.getY();
		return pixelByX > pixelByY;
	}

	/**
	 * Calculate the start point of the crop area. The point will take the given focal point and the image size into account.
	 * 
	 * @param alignX
	 * @param targetSize
	 * @param imageSize
	 * @param focalPoint
	 * @return Calculated start point or null if cropping is not possible / not needed
	 */
	protected Point calculateCropStart(boolean alignX, Point targetSize, Point imageSize, FocalPoint focalPoint) {

		// Cropping is actually not needed if the source already matches the target size
		if (targetSize.equals(imageSize)) {
			return null;
		}

		Point point = focalPoint.convertToAbsolutePoint(imageSize);

		// Next we need to crop the image in order to achieve the final targeted size
		// TODO next crop the other dimension using the focal point and choose the crop area closest to the middle of the needed focal point axis.
		int startX = 0;
		int startY = 0;
		if (!alignX) {
			int half = targetSize.getX() / 2;
			startX = point.getX() - half;
			// Clamp the start point to zero if the start-y value would be outside of the image.
			if (startX < 0) {
				startX = 0;
			} else if (startX + targetSize.getX() > imageSize.getX()) {
				startX = imageSize.getX() - targetSize.getX();
			}
		} else {
			int half = targetSize.getY() / 2;
			startY = point.getY() - half;
			// Clamp the start point to zero if the start-y value would be outside of the image.
			if (startY < 0) {
				startY = 0;
			} else if (startY + targetSize.getY() > imageSize.getY()) {
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
	protected void drawFocusPointAxis(BufferedImage img, FocalPoint focalPoint) {
		Point point = focalPoint.convertToAbsolutePoint(new Point(img.getWidth(), img.getHeight()));

		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(3));
		// x-axis of focal point
		g2.drawLine(point.getX(), 0, point.getX(), img.getHeight());
		// y-axis of focal point
		g2.drawLine(0, point.getY(), img.getWidth(), point.getY());
	}

	/**
	 * Calculate the new size for the initial resize operation.
	 * 
	 * @param imageSize
	 * @param targetSize
	 * @return
	 */
	protected Point calculateResize(Point imageSize, Point targetSize) {
		Integer targetWidth = targetSize.getX();
		Integer targetHeight = targetSize.getY();

		// Determine which image dimension is nearest to the target dimension.
		// The image should always be larger then the target size since the
		// remaining additional area will be cropped
		boolean alignX = calculateAlignment(imageSize, targetSize);

		double aspectRatio = imageSize.getRatio();
		int resizeX = targetWidth;
		int resizeY = targetHeight;
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

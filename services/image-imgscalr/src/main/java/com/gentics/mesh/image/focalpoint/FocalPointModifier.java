package com.gentics.mesh.image.focalpoint;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.twelvemonkeys.image.ResampleOp;

/**
 * Implementation of the focal point modifier. This modifier will:
 * <ul>
 * <li>Resize the image so that it exceeds the targeted size in one dimension
 * <li>Crop the image in a way so that the targeted size is reached
 * <li>Or apply a zoom and crop to the image
 * </ul>
 */
public class FocalPointModifier {

	private final ImageManipulatorOptions options;
	public FocalPointModifier(ImageManipulatorOptions options) {
		this.options = options;
	}

	/**
	 * First resize the image and later crop the image to focus the focal point.
	 * 
	 * @param img
	 * @return resized and cropped image
	 */
	public BufferedImage apply(BufferedImage img, ImageManipulation parameters) {
		FocalPoint focalPoint = parameters.getFocalPoint();
		if (focalPoint == null) {
			return img;
		}

		if (parameters instanceof ImageManipulationParameters && ((ImageManipulationParameters) parameters).getFocalPointDebug()) {
			drawFocusPointAxis(img, focalPoint);
		}

		// Validate the focal point position
		Point imageSize = new Point(img.getWidth(), img.getHeight());
		Point absFocalPoint = parameters.getFocalPoint().convertToAbsolutePoint(imageSize);
		if (!absFocalPoint.isWithinBoundsOf(imageSize)) {
			throw error(BAD_REQUEST, "image_error_focalpoint_out_of_bounds", focalPoint.toString(), imageSize.toString());
		}

		Point targetSize = parameters.getSize();
		Float zoomFactor = parameters.getFocalPointZoom();
		BufferedImage zoomedImg = applyZoom(img, zoomFactor, focalPoint, targetSize);
		// Apply the regular focal point logic if the zoom was not applied. Otherwise the image is already cropped and handled correctly.
		if (zoomedImg == null) {
			if (targetSize == null) {
				throw error(BAD_REQUEST, "image_error_focalpoint_target_missing");
			}

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

		} else {
			img = zoomedImg;
		}
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
	 * @param targetSize
	 * @return Zoomed image or null if zoom factor is invalid
	 */
	private BufferedImage applyZoom(BufferedImage img, Float zoomFactor, FocalPoint focalPoint, Point targetSize) {
		if (zoomFactor == null || zoomFactor <= 1) {
			return null;
		}
		int x = img.getWidth();
		int y = img.getHeight();

		// Use the original image size as target size if no specific target size has been specified.
		if (targetSize == null) {
			targetSize = new Point(x, y);
		}

		// We zoom by creating a sub image of the original image. The section size will be defined by the zoom factor and the target size.
		int zw = Math.round(targetSize.getX() / zoomFactor);
		int zh = Math.round(targetSize.getY() / zoomFactor);

		// If the calculated sub image size is bigger than the original image, the zoom factor is not enough for target size.
		if (zw > x || zh > y) {
			throw error(BAD_REQUEST, "image_error_target_too_large_for_zoom");
		}

		Point zstart = calculateZoomStart(focalPoint, new Point(x, y), zw, zh);

		// Now create the subimage
		img = img.getSubimage(zstart.getX(), zstart.getY(), zw, zh);

		// And resize it back to the target dimension and thus applying the zoom
		img = applyResize(img, targetSize);

		return img;
	}

	/**
	 * Calculate the zoom subimage start coordinates. The coordinates will take the focal point and the zoom area size into account.
	 * 
	 * @param focalPoint
	 * @param imageSize
	 * @param zoomWidth
	 * @param zoomHeight
	 * @return
	 */
	protected Point calculateZoomStart(FocalPoint focalPoint, Point imageSize, int zoomWidth, int zoomHeight) {
		int x = imageSize.getX();
		int y = imageSize.getY();

		Point absFocalPoint = focalPoint.convertToAbsolutePoint(imageSize);
		// We need to determine the start point of our sub image in relation to the focal point
		int zx = absFocalPoint.getX() - (zoomWidth / 2);

		// Clamp the bounds so that the start point will not exceed the image bounds in relation to the sub image width
		if (zx < 0) {
			zx = 0;
		} else if (zx > x - zoomWidth) {
			zx = x - zoomWidth;
		}

		int zy = absFocalPoint.getY() - (zoomHeight / 2);

		// Clamp the bounds so that the start point will not exceed the image bounds in relation to the sub image height
		if (zy < 0) {
			zy = 0;
		} else if (zy > y - zoomHeight) {
			zy = y - zoomHeight;
		}
		return new Point(zx, zy);
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
			return Scalr.apply(img, new ResampleOp(size.getX(), size.getY(), options.getResampleFilter().getFilter()));
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

		float strokeWidth = 3f;
		Graphics2D g = img.createGraphics();
		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(strokeWidth));
		// x-axis of focal point
		g.drawLine(point.getX(), 0, point.getX(), img.getHeight());
		// y-axis of focal point
		g.drawLine(0, point.getY(), img.getWidth(), point.getY());
		g.dispose();
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

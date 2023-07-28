package com.gentics.mesh.parameter.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.util.NumberUtils;

/**
 * Generic image manipulation contract.
 * 
 * @author plyhun
 *
 */
public interface ImageManipulation {

	/**
	 * Return the image width.
	 *
	 * @return
	 */
	String getWidth();

	/**
	 * Set the image width.
	 *
	 * @param width
	 * @return Fluent API
	 */
	ImageManipulation setWidth(String width);

	/**
	 * Set the image width.
	 *
	 * @param width
	 * @return Fluent API
	 */
	ImageManipulation setWidth(Integer width);

	/**
	 * Return the image height.
	 *
	 * @return
	 */
	String getHeight();

	/**
	 * Set the image height.
	 *
	 * @param height
	 * @return Fluent API
	 */
	ImageManipulation setHeight(String height);

	/**
	 * Set the image height.
	 *
	 * @param height
	 * @return Fluent API
	 */
	ImageManipulation setHeight(Integer height);

	/**
	 * Set the target size of the image.
	 *
	 * @param width
	 * @param height
	 * @return Fluent API
	 */
	default ImageManipulation setSize(int width, int height) {
		setWidth(String.valueOf(width));
		setHeight(String.valueOf(height));
		return this;
	}

	/**
	 * Set the target size of the image.
	 *
	 * @param size
	 * @return Fluent API
	 */
	default ImageManipulation setSize(Point size) {
		return setSize(size.getX(), size.getY());
	}

	/**
	 * Return the image size.
	 *
	 * @return Image size or null when width or height are missing
	 */
	default Point getSize() {
		Integer w = NumberUtils.toInteger(getWidth(), null);
		Integer h = NumberUtils.toInteger(getHeight(), null);
		if (w == null || h == null) {
			return null;
		}
		return new Point(w, h);
	}

	/**
	 * Returns the rect crop area parameter value.
	 *
	 * @return Configured image crop area rectangle
	 */
	ImageRect getRect();

	/**
	 * Set the crop area.
	 *
	 * @param startX
	 * @param startY
	 * @param height
	 * @param width
	 * @return Fluent API
	 */
	default ImageManipulation setRect(int startX, int startY, int height, int width) {
		ImageRect rect = new ImageRect(startX, startY, height, width);
		setRect(rect);
		return this;
	}

	/**
	 * Set the crop area.
	 *
	 * @param rect
	 * @return
	 */
	ImageRect setRect(ImageRect rect);

	/**
	 * Return the crop mode parameter value.
	 *
	 * @return
	 */
	CropMode getCropMode();

	/**
	 * Set the crop mode parameter.
	 *
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulation setCropMode(String mode) {
		CropMode cropMode = CropMode.get(mode);
		if (cropMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", "cropMode", mode);
		}
		return setCropMode(cropMode);
	}

	/**
	 * Set the crop mode parameter.
	 *
	 * @param mode
	 * @return Fluent API
	 */
	ImageManipulation setCropMode(CropMode mode);

	/**
	 * Return the resize mode parameter value.
	 *
	 * @return
	 */
	ResizeMode getResizeMode();

	/**
	 * Set the resize mode parameter.
	 *
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulation setResizeMode(String mode) {
		ResizeMode resizeMode = ResizeMode.get(mode);
		if (resizeMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", "resizeMode", mode);
		}
		return setResizeMode(resizeMode);
	}

	/**
	 * Set the resize mode parameter.
	 *
	 * @param mode
	 * @return Fluent API
	 */
	ImageManipulation setResizeMode(ResizeMode mode);

	/**
	 * Check whether focal point parameters have been set.
	 *
	 * @return
	 */
	boolean hasFocalPoint();

	/**
	 * Get the focal point that has been set in the image parameter.
	 *
	 * @return
	 */
	FocalPoint getFocalPoint();

	/**
	 * Get the focal point zoom factor.
	 *
	 * @return
	 */
	Float getFocalPointZoom();

	/**
	 * Set the focal point.
	 *
	 * @param point
	 * @return Fluent API
	 */
	ImageManipulation setFocalPoint(FocalPoint point);

	/**
	 * Set the focal point.
	 *
	 * @param x
	 * @param y
	 * @return Fluent API
	 */
	default ImageManipulation setFocalPoint(float x, float y) {
		return setFocalPoint(new FocalPoint(x, y));
	}

	/**
	 * Set the focal point zoom factor.
	 *
	 * @param factor
	 * @return Fluent API
	 */
	ImageManipulation setFocalPointZoom(Float factor);

	/**
	 * Validates whether the focal point was fully specified.
	 *
	 * @return Fluent API
	 */
	ImageManipulation validateFocalPointParameter();

	/**
	 * Check whether all required crop parameters have been set.
	 *
	 * @param options
	 * @return Fluent API
	 */
	default ImageManipulation validateLimits(ImageManipulatorOptions options) {
		int width = NumberUtils.toInt(getWidth(), 0);
		int height = NumberUtils.toInt(getHeight(), 0);
		if (getWidth() != null && options.getMaxWidth() != null && options.getMaxWidth() > 0 && width > options.getMaxWidth()) {
			throw error(BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(getWidth()));
		}
		if (getHeight() != null && options.getMaxHeight() != null && options.getMaxHeight() > 0 && height > options.getMaxHeight()) {
			throw error(BAD_REQUEST, "image_error_height_limit_exceeded", String.valueOf(options.getMaxHeight()), String.valueOf(getHeight()));
		}
		return this;
	}

	/**
	 * Check whether any resize or crop param has been set.
	 *
	 * @return
	 */
	default boolean hasResizeParams() {
		return getHeight() != null || getWidth() != null || getCropMode() != null;
	}
}

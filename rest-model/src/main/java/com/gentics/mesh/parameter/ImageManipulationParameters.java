package com.gentics.mesh.parameter;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.NumberUtils.toInteger;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;

public interface ImageManipulationParameters extends ParameterProvider {

	public static final String WIDTH_QUERY_PARAM_KEY = "w";

	public static final String HEIGHT_QUERY_PARAM_KEY = "h";

	public static final String FOCAL_POINT_X_QUERY_PARAM_KEY = "fpx";

	public static final String FOCAL_POINT_Y_QUERY_PARAM_KEY = "fpy";

	public static final String FOCAL_POINT_Z_QUERY_PARAM_KEY = "fpz";

	public static final String RECT_QUERY_PARAM_KEY = "rect";

	public static final String CROP_MODE_QUERY_PARAM_KEY = "crop";
	
	public static final String RESIZE_MODE_QUERY_PARAM_KEY = "resize";

	public static final String FOCAL_POINT_DEBUG_PARAM_KEY = "fpdebug";

	/**
	 * Return the image width.
	 * 
	 * @return
	 */
	default Integer getWidth() {
		return toInteger(getParameter(WIDTH_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the image width.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	default ImageManipulationParameters setWidth(Integer width) {
		setParameter(WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		return this;
	}

	/**
	 * Return the image height.
	 * 
	 * @return
	 */
	default Integer getHeight() {
		return toInteger(getParameter(HEIGHT_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the image height.
	 * 
	 * @param height
	 * @return Fluent API
	 */
	default ImageManipulationParameters setHeight(Integer height) {
		setParameter(HEIGHT_QUERY_PARAM_KEY, String.valueOf(height));
		return this;
	}

	/**
	 * Set the target size of the image.
	 * 
	 * @param width
	 * @param height
	 * @return Fluent API
	 */
	default ImageManipulationParameters setSize(int width, int height) {
		setWidth(width);
		setHeight(height);
		return this;
	}

	/**
	 * Set the target size of the image.
	 * 
	 * @param size
	 * @return Fluent API
	 */
	default ImageManipulationParameters setSize(Point size) {
		return setSize(size.getX(), size.getY());
	}

	/**
	 * Return the image size.
	 * 
	 * @return Image size or null when width or height are missing
	 */
	default Point getSize() {
		Integer w = getWidth();
		Integer h = getHeight();
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
	default ImageRect getRect() {
		String rect = getParameter(RECT_QUERY_PARAM_KEY);
		return rect == null ? null : new ImageRect(rect);
	}

	/**
	 * Set the crop area.
	 * 
	 * @param startX
	 * @param startY
	 * @param height
	 * @param width
	 * @return Fluent API
	 */
	default ImageManipulationParameters setRect(int startX, int startY, int height, int width) {
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
	default ImageRect setRect(ImageRect rect) {
		if (rect == null) {
			setParameter(RECT_QUERY_PARAM_KEY, null);
		} else {
			setParameter(RECT_QUERY_PARAM_KEY, rect.toString());
		}
		return rect;
	}

	/**
	 * Return the crop mode parameter value.
	 * 
	 * @return
	 */
	default CropMode getCropMode() {
		String mode = getParameter(CROP_MODE_QUERY_PARAM_KEY);
		return CropMode.get(mode);
	}

	/**
	 * Set the crop mode parameter.
	 * 
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulationParameters setCropMode(String mode) {
		CropMode cropMode = CropMode.get(mode);
		if (cropMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", CROP_MODE_QUERY_PARAM_KEY, mode);
		}
		return setCropMode(cropMode);
	}

	/**
	 * Set the crop mode parameter.
	 * 
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulationParameters setCropMode(CropMode mode) {
		if(mode != null) {
			setParameter(CROP_MODE_QUERY_PARAM_KEY, mode.getKey());	
		}
		return this;
	}

	/**
	 * Return the resize mode parameter value.
	 * 
	 * @return
	 */
	default ResizeMode getResizeMode() {
		String mode = getParameter(RESIZE_MODE_QUERY_PARAM_KEY);
		return ResizeMode.get(mode) == null ? ResizeMode.SMART : ResizeMode.get(mode);
	}

	/**
	 * Set the resize mode parameter.
	 * 
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulationParameters setResizeMode(String mode) {
		ResizeMode resizeMode = ResizeMode.get(mode);
		if (resizeMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", RESIZE_MODE_QUERY_PARAM_KEY, mode);
		}
		return setResizeMode(resizeMode);
	}

	/**
	 * Set the resize mode parameter.
	 * 
	 * @param mode
	 * @return Fluent API
	 */
	default ImageManipulationParameters setResizeMode(ResizeMode mode) {
		if(mode != null) {
			setParameter(RESIZE_MODE_QUERY_PARAM_KEY, mode.getKey());
		}
		return this;
	}
	
	/**
	 * Check whether focal point parameters have been set.
	 * 
	 * @return
	 */
	default boolean hasFocalPoint() {
		String x = getParameter(FOCAL_POINT_X_QUERY_PARAM_KEY);
		String y = getParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY);
		return x != null && y != null;
	}

	/**
	 * Get the focal point that has been set in the image parameter.
	 * 
	 * @return
	 */
	default FocalPoint getFocalPoint() {
		String x = getParameter(FOCAL_POINT_X_QUERY_PARAM_KEY);
		String y = getParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY);
		// If either x or y has not been set, use the center of the image for the respective focal point part.
		Float fpx = x != null ? Float.valueOf(x) : 0.5F;
		Float fpy = y != null ? Float.valueOf(y) : 0.5F;
		return new FocalPoint(fpx, fpy);
	}

	/**
	 * Get the focal point zoom factor.
	 * 
	 * @return
	 */
	default Float getFocalPointZoom() {
		String z = getParameter(FOCAL_POINT_Z_QUERY_PARAM_KEY);
		if (z == null) {
			return null;
		} else {
			return Float.valueOf(z);
		}
	}

	/**
	 * Set the focal point.
	 * 
	 * @param point
	 * @return Fluent API
	 */
	default ImageManipulationParameters setFocalPoint(FocalPoint point) {
		if (point == null) {
			setParameter(FOCAL_POINT_X_QUERY_PARAM_KEY, null);
			setParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY, null);
		} else {
			setParameter(FOCAL_POINT_X_QUERY_PARAM_KEY, String.valueOf(point.getX()));
			setParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY, String.valueOf(point.getY()));
		}
		return this;
	}

	/**
	 * Set the focal point.
	 * 
	 * @param x
	 * @param y
	 * @return Fluent API
	 */
	default ImageManipulationParameters setFocalPoint(float x, float y) {
		return setFocalPoint(new FocalPoint(x, y));
	}

	/**
	 * Set the focal point zoom factor.
	 * 
	 * @param factor
	 * @return Fluent API
	 */
	default ImageManipulationParameters setFocalPointZoom(Float factor) {
		if (factor == null) {
			setParameter(FOCAL_POINT_Z_QUERY_PARAM_KEY, null);
		} else {
			setParameter(FOCAL_POINT_Z_QUERY_PARAM_KEY, String.valueOf(factor));
		}
		return this;
	}

	/**
	 * Set the focal point debug flag.
	 * 
	 * @param flag
	 * @return
	 */
	default ImageManipulationParameters setFocalPointDebug(Boolean flag) {
		if (flag == null) {
			setParameter(FOCAL_POINT_DEBUG_PARAM_KEY, null);
		} else {
			setParameter(FOCAL_POINT_DEBUG_PARAM_KEY, flag.toString());
		}
		return this;
	}

	/**
	 * Return the focal point debug flag.
	 * 
	 * @return
	 */
	default boolean getFocalPointDebug() {
		String flag = getParameter(FOCAL_POINT_DEBUG_PARAM_KEY);
		return Boolean.valueOf(flag);
	}

	/**
	 * Validates whether the focal point was fully specified.
	 * 
	 * @return Fluent API
	 */
	default ImageManipulationParameters validateFocalPointParameter() {
		String x = getParameter(FOCAL_POINT_X_QUERY_PARAM_KEY);
		String y = getParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY);
		if (x == null && y != null) {
			throw error(BAD_REQUEST, "image_error_incomplete_focalpoint_parameters", FOCAL_POINT_X_QUERY_PARAM_KEY);
		} else if (y == null && x != null) {
			throw error(BAD_REQUEST, "image_error_incomplete_focalpoint_parameters", FOCAL_POINT_Y_QUERY_PARAM_KEY);
		}
		return this;
	}

	/**
	 * Check whether all required crop parameters have been set.
	 * 
	 * @param options
	 * @return Fluent API
	 */
	default ImageManipulationParameters validateLimits(ImageManipulatorOptions options) {
		if (getWidth() != null && options.getMaxWidth() != null && options.getMaxWidth() > 0 && getWidth() > options.getMaxWidth()) {
			throw error(BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(getWidth()));
		}
		if (getHeight() != null && options.getMaxHeight() != null && options.getMaxHeight() > 0 && getHeight() > options.getMaxHeight()) {
			throw error(BAD_REQUEST, "image_error_height_limit_exceeded", String.valueOf(options.getMaxHeight()), String.valueOf(getHeight()));
		}
		return this;
	}

	/**
	 * Generate cache key.
	 * 
	 * @return
	 */
	default String getCacheKey() {
		StringBuilder builder = new StringBuilder();

		if (getRect() != null) {
			builder.append("rect" + getRect().toString());
		}
		if (getCropMode() != null) {
			builder.append("crop" + getCropMode());
		}
		if (getResizeMode() != null) {
			builder.append("resize" + getResizeMode());
		}
		if (getWidth() != null) {
			builder.append("rw" + getWidth());
		}
		if (getHeight() != null) {
			builder.append("rh" + getHeight());
		}
		if (getFocalPoint() != null) {
			builder.append("fp" + getFocalPoint().toString());
		}
		if (getFocalPointDebug()) {
			builder.append("fpdebug");
		}

		if (getFocalPointZoom() != null) {
			builder.append("fpz" + getFocalPointZoom());
		}
		
		return builder.toString();
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

package com.gentics.mesh.parameter;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Interface for image manipulation query parameters. Crop and resize parameters for image manipulation.
 */
public interface ImageManipulationParameters extends ImageManipulation, ParameterProvider {

	public static final String WIDTH_QUERY_PARAM_KEY = "w";

	public static final String HEIGHT_QUERY_PARAM_KEY = "h";

	public static final String FOCAL_POINT_X_QUERY_PARAM_KEY = "fpx";

	public static final String FOCAL_POINT_Y_QUERY_PARAM_KEY = "fpy";

	public static final String FOCAL_POINT_Z_QUERY_PARAM_KEY = "fpz";

	public static final String RECT_QUERY_PARAM_KEY = "rect";

	public static final String CROP_MODE_QUERY_PARAM_KEY = "crop";

	public static final String RESIZE_MODE_QUERY_PARAM_KEY = "resize";

	public static final String FOCAL_POINT_DEBUG_PARAM_KEY = "fpdebug";

	public static final String AUTO = "auto";

	@Override
	default String getWidth() {
		return getParameter(WIDTH_QUERY_PARAM_KEY);
	}

	@Override
	default ImageManipulationParameters setWidth(String width) {
		setParameter(WIDTH_QUERY_PARAM_KEY, width);
		return this;
	}

	@Override
	default ImageManipulationParameters setWidth(Integer width) {
		setParameter(WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		return this;
	}

	@Override
	default String getHeight() {
		return getParameter(HEIGHT_QUERY_PARAM_KEY);
	}

	@Override
	default ImageManipulationParameters setHeight(String height) {
		setParameter(HEIGHT_QUERY_PARAM_KEY, height);
		return this;
	}

	@Override
	default ImageManipulationParameters setHeight(Integer height) {
		setParameter(HEIGHT_QUERY_PARAM_KEY, String.valueOf(height));
		return this;
	}

	@Override
	default ImageRect getRect() {
		String rect = getParameter(RECT_QUERY_PARAM_KEY);
		return rect == null ? null : new ImageRect(rect);
	}

	@Override
	default ImageRect setRect(ImageRect rect) {
		if (rect == null) {
			setParameter(RECT_QUERY_PARAM_KEY, null);
		} else {
			setParameter(RECT_QUERY_PARAM_KEY, rect.toString());
		}
		return rect;
	}

	@Override
	default CropMode getCropMode() {
		String mode = getParameter(CROP_MODE_QUERY_PARAM_KEY);
		return CropMode.get(mode);
	}

	@Override
	default ImageManipulationParameters setCropMode(String mode) {
		CropMode cropMode = CropMode.get(mode);
		if (cropMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", CROP_MODE_QUERY_PARAM_KEY, mode);
		}
		return setCropMode(cropMode);
	}

	@Override
	default ImageManipulationParameters setCropMode(CropMode mode) {
		if (mode != null) {
			setParameter(CROP_MODE_QUERY_PARAM_KEY, mode.getKey());
		}
		return this;
	}

	@Override
	default ResizeMode getResizeMode() {
		String mode = getParameter(RESIZE_MODE_QUERY_PARAM_KEY);
		return ResizeMode.get(mode) == null ? ResizeMode.SMART : ResizeMode.get(mode);
	}

	@Override
	default ImageManipulationParameters setResizeMode(String mode) {
		ResizeMode resizeMode = ResizeMode.get(mode);
		if (resizeMode == null) {
			throw error(BAD_REQUEST, "image_error_parameter_invalid", RESIZE_MODE_QUERY_PARAM_KEY, mode);
		}
		return setResizeMode(resizeMode);
	}

	@Override
	default ImageManipulationParameters setResizeMode(ResizeMode mode) {
		if (mode != null) {
			setParameter(RESIZE_MODE_QUERY_PARAM_KEY, mode.getKey());
		}
		return this;
	}

	@Override
	default boolean hasFocalPoint() {
		String x = getParameter(FOCAL_POINT_X_QUERY_PARAM_KEY);
		String y = getParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY);
		return x != null && y != null;
	}

	@Override
	default FocalPoint getFocalPoint() {
		String x = getParameter(FOCAL_POINT_X_QUERY_PARAM_KEY);
		String y = getParameter(FOCAL_POINT_Y_QUERY_PARAM_KEY);
		// If either x or y has not been set, use the center of the image for the respective focal point part.
		Float fpx = x != null ? Float.valueOf(x) : 0.5F;
		Float fpy = y != null ? Float.valueOf(y) : 0.5F;
		return new FocalPoint(fpx, fpy);
	}

	@Override
	default Float getFocalPointZoom() {
		String z = getParameter(FOCAL_POINT_Z_QUERY_PARAM_KEY);
		if (z == null) {
			return null;
		} else {
			return Float.valueOf(z);
		}
	}

	@Override
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

	@Override
	default ImageManipulationParameters setFocalPoint(float x, float y) {
		return setFocalPoint(new FocalPoint(x, y));
	}

	@Override
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

	@Override
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

	@Override
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

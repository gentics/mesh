package com.gentics.mesh.parameter;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.NumberUtils.toInteger;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;

public interface ImageManipulationParameters extends ParameterProvider {

	public static final String WIDTH_QUERY_PARAM_KEY = "width";
	public static final String HEIGHT_QUERY_PARAM_KEY = "height";

	public static final String CROP_X_QUERY_PARAM_KEY = "cropx";
	public static final String CROP_Y_QUERY_PARAM_KEY = "cropy";

	public static final String CROP_HEIGHT_QUERY_PARAM_KEY = "croph";
	public static final String CROP_WIDTH_QUERY_PARAM_KEY = "cropw";

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
	 * Return the crop x-axis start coordinate.
	 * 
	 * @return
	 */
	default Integer getStartx() {
		return toInteger(getParameter(CROP_X_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the crop x-axis start coordinate.
	 * 
	 * @param startx
	 * @return Fluent API
	 */
	default ImageManipulationParameters setStartx(Integer startx) {
		setParameter(CROP_X_QUERY_PARAM_KEY, String.valueOf(startx));
		return this;
	}

	/**
	 * Return the crop y-axis start coordinate.
	 * 
	 * @return
	 */
	default Integer getStarty() {
		return toInteger(getParameter(CROP_Y_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the crop y-axis start coordinate.
	 * 
	 * @param starty
	 * @return Fluent API
	 */
	default ImageManipulationParameters setStarty(Integer starty) {
		setParameter(CROP_Y_QUERY_PARAM_KEY, String.valueOf(starty));
		return this;
	}

	/**
	 * Return the crop height.
	 * 
	 * @return
	 */
	default Integer getCroph() {
		return toInteger(getParameter(CROP_HEIGHT_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the crop height.
	 * 
	 * @param croph
	 * @return Fluent API
	 */
	default ImageManipulationParameters setCroph(Integer croph) {
		setParameter(CROP_HEIGHT_QUERY_PARAM_KEY, String.valueOf(croph));
		return this;
	}

	/**
	 * Return the crop width.
	 * 
	 * @return
	 */
	default Integer getCropw() {
		return toInteger(getParameter(CROP_WIDTH_QUERY_PARAM_KEY), null);
	}

	/**
	 * Set the crop width.
	 * 
	 * @param cropw
	 * @return Fluent API
	 */
	default ImageManipulationParameters setCropw(Integer cropw) {
		setParameter(CROP_WIDTH_QUERY_PARAM_KEY, String.valueOf(cropw));
		return this;
	}

	/**
	 * Check whether all required crop parameters have been set.
	 * 
	 * @param options
	 */
	default void validateLimits(ImageManipulatorOptions options) {
		if (getWidth() != null && options.getMaxWidth() != null && options.getMaxWidth() > 0 && getWidth() > options.getMaxWidth()) {
			throw error(BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(getWidth()));
		}
		if (getHeight() != null && options.getMaxHeight() != null && options.getMaxHeight() > 0 && getHeight() > options.getMaxHeight()) {
			throw error(BAD_REQUEST, "image_error_height_limit_exceeded", String.valueOf(options.getMaxHeight()), String.valueOf(getHeight()));
		}
	}

	/**
	 * Check whether all needed crop parameters have been set.
	 * 
	 * @return
	 */
	default boolean hasAllCropParameters() {
		return getCroph() != null && getCropw() != null && getStartx() != null && getStarty() != null;
	}

	/**
	 * Validate the image crop parameters and check whether those would exceed the source image dimensions.
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 */
	default void validateCropBounds(int imageWidth, int imageHeight) {
		if (getStartx() + getCropw() > imageWidth || getStarty() + getCroph() > imageHeight) {
			throw error(BAD_REQUEST, "image_error_crop_out_of_bounds", String.valueOf(imageWidth), String.valueOf(imageHeight));
		}
	}

	/**
	 * Generate cache key.
	 * 
	 * @return
	 */
	default String getCacheKey() {
		StringBuilder builder = new StringBuilder();

		if (getStartx() != null) {
			builder.append("cx" + getStartx());
		}
		if (getStarty() != null) {
			builder.append("cy" + getStarty());
		}
		if (getCropw() != null) {
			builder.append("cw" + getCropw());
		}
		if (getCroph() != null) {
			builder.append("ch" + getCroph());
		}
		if (getWidth() != null) {
			builder.append("rw" + getWidth());
		}
		if (getHeight() != null) {
			builder.append("rh" + getHeight());
		}
		return builder.toString();
	}

	/**
	 * Check whether any of the parameters is set.
	 * 
	 * @return
	 */
	default boolean isSet() {
		return getWidth() != null || getHeight() != null || getCroph() != null || getCropw() != null || getStartx() != null || getStarty() != null;
	}

}

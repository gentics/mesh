package com.gentics.mesh.query.impl;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.util.NumberUtils.toInteger;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.util.HttpQueryUtils;

public class ImageRequestParameter implements QueryParameterProvider {

	public static final String WIDTH_QUERY_PARAM_KEY = "width";
	public static final String HEIGHT_QUERY_PARAM_KEY = "height";

	public static final String CROP_X_QUERY_PARAM_KEY = "cropx";
	public static final String CROP_Y_QUERY_PARAM_KEY = "cropy";

	public static final String CROP_HEIGHT_QUERY_PARAM_KEY = "croph";
	public static final String CROP_WIDTH_QUERY_PARAM_KEY = "cropw";

	private Integer width;
	private Integer height;
	private Integer startx;
	private Integer starty;
	private Integer cropw;
	private Integer croph;

	/**
	 * Return the image width.
	 * 
	 * @return
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the image width.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	public ImageRequestParameter setWidth(Integer width) {
		this.width = width;
		return this;
	}

	/**
	 * Return the image height.
	 * 
	 * @return
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the image height.
	 * 
	 * @param height
	 * @return Fluent API
	 */
	public ImageRequestParameter setHeight(Integer height) {
		this.height = height;
		return this;
	}

	/**
	 * Return the crop x-axis start coordinate.
	 * 
	 * @return
	 */
	public Integer getStartx() {
		return startx;
	}

	/**
	 * Set the crop x-axis start coordinate.
	 * 
	 * @param startx
	 * @return Fluent API
	 */
	public ImageRequestParameter setStartx(Integer startx) {
		this.startx = startx;
		return this;
	}

	/**
	 * Return the crop y-axis start coordinate.
	 * 
	 * @return
	 */
	public Integer getStarty() {
		return starty;
	}

	/**
	 * Set the crop y-axis start coordinate.
	 * 
	 * @param starty
	 * @return Fluent API
	 */
	public ImageRequestParameter setStarty(Integer starty) {
		this.starty = starty;
		return this;
	}

	/**
	 * Return the crop height.
	 * 
	 * @return
	 */
	public Integer getCroph() {
		return croph;
	}

	/**
	 * Set the crop height.
	 * 
	 * @param croph
	 * @return Fluent API
	 */
	public ImageRequestParameter setCroph(Integer croph) {
		this.croph = croph;
		return this;
	}

	/**
	 * Return the crop width.
	 * 
	 * @return
	 */
	public Integer getCropw() {
		return cropw;
	}

	/**
	 * Set the crop width.
	 * 
	 * @param cropw
	 * @return Fluent API
	 */
	public ImageRequestParameter setCropw(Integer cropw) {
		this.cropw = cropw;
		return this;
	}

	public static ImageRequestParameter fromQuery(String query) {
		ImageRequestParameter parameter = new ImageRequestParameter();
		Map<String, String> parameters = HttpQueryUtils.splitQuery(query);
		parameter.setHeight(toInteger(parameters.get(HEIGHT_QUERY_PARAM_KEY), null));
		parameter.setWidth(toInteger(parameters.get(WIDTH_QUERY_PARAM_KEY), null));
		parameter.setCroph(toInteger(parameters.get(CROP_HEIGHT_QUERY_PARAM_KEY), null));
		parameter.setCropw(toInteger(parameters.get(CROP_WIDTH_QUERY_PARAM_KEY), null));
		parameter.setStartx(toInteger(parameters.get(CROP_X_QUERY_PARAM_KEY), null));
		parameter.setStarty(toInteger(parameters.get(CROP_Y_QUERY_PARAM_KEY), null));
		return parameter;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (width != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(WIDTH_QUERY_PARAM_KEY + "=" + width);
		}
		if (height != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(HEIGHT_QUERY_PARAM_KEY + "=" + height);
		}

		// crop
		if (startx != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(CROP_X_QUERY_PARAM_KEY + "=" + startx);
		}
		if (starty != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(CROP_Y_QUERY_PARAM_KEY + "=" + starty);
		}
		if (croph != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(CROP_HEIGHT_QUERY_PARAM_KEY + "=" + croph);
		}
		if (cropw != null) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(CROP_WIDTH_QUERY_PARAM_KEY + "=" + cropw);
		}

		return query.toString();
	}

	/**
	 * Check whether any of the parameters is set.
	 * 
	 * @return
	 */
	public boolean isSet() {
		return width != null || height != null || croph != null || cropw != null || startx != null || starty != null;
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

	/**
	 * * Validate the set parameters and throw an exception when an invalid set of parameters has been detected.
	 */
	public void validate() {
		if (width != null && width < 1) {
			throw error(BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		}

		if (height != null && height < 1) {
			throw error(BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.HEIGHT_QUERY_PARAM_KEY, String.valueOf(height));
		}

		//  Check whether all required crop parameters have been set when at least one crop parameter has been set.
		boolean hasOneCropParameter = croph != null || cropw != null || startx != null || starty != null;
		if (hasOneCropParameter) {
			//Check whether all required crop parameters have been set.
			if (!hasAllCropParameters()) {
				throw error(BAD_REQUEST, "image_error_incomplete_crop_parameters");
			}

			if (croph != null && croph <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.CROP_HEIGHT_QUERY_PARAM_KEY, String.valueOf(croph));
			}

			if (cropw != null && cropw <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageRequestParameter.CROP_WIDTH_QUERY_PARAM_KEY, String.valueOf(cropw));
			}

			if (startx != null && startx <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageRequestParameter.CROP_X_QUERY_PARAM_KEY, String.valueOf(startx));
			}

			if (starty != null && starty <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageRequestParameter.CROP_Y_QUERY_PARAM_KEY, String.valueOf(starty));
			}

		}

	}

	/**
	 * Check whether all required crop parameters have been set.
	 * 
	 * @return
	 */
	public boolean hasAllCropParameters() {
		return croph != null && cropw != null && startx != null && starty != null;
	}

	/**
	 * Validate the image crop parameters and check whether those would exceed the source image dimensions.
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 */
	public void validateCropBounds(int imageWidth, int imageHeight) {
		if (startx + cropw > imageWidth || starty + croph > imageHeight) {
			throw error(BAD_REQUEST, "image_error_crop_out_of_bounds", String.valueOf(imageWidth), String.valueOf(imageHeight));
		}
	}

	/**
	 * Generate cache key.
	 * 
	 * @return
	 */
	public String getCacheKey() {

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

	public void validateLimits(ImageManipulatorOptions options) {
		if (getWidth() != null && options.getMaxWidth() != null && options.getMaxWidth() > 0 && getWidth() > options.getMaxWidth()) {
			throw error(BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(getWidth()));
		}
		if (getHeight() != null && options.getMaxHeight() != null && options.getMaxHeight() > 0 && getHeight() > options.getMaxHeight()) {
			throw error(BAD_REQUEST, "image_error_height_limit_exceeded", String.valueOf(options.getMaxHeight()), String.valueOf(getHeight()));
		}

	}

}
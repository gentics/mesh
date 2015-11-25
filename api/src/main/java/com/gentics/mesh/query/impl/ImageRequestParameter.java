package com.gentics.mesh.query.impl;

import static com.gentics.mesh.util.NumberUtils.toInteger;

import java.util.Map;

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
	 * Check whether all required crop parameters have been set when at least one crop parameter has been set.
	 * 
	 * @return
	 */
	public boolean hasValidOrNoneCropParameters() {
		boolean oneSet = croph != null || cropw != null || startx != null || starty != null;
		boolean allSet = hasAllCropParameters();
		return oneSet ? allSet : true;
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
	 * Generate cache key.
	 * 
	 * @param parameters
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

}
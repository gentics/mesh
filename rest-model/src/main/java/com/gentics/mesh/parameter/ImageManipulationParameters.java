package com.gentics.mesh.parameter;

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
	Integer getWidth();

	/**
	 * Set the image width.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	ImageManipulationParameters setWidth(Integer width);

	/**
	 * Return the image height.
	 * 
	 * @return
	 */
	Integer getHeight();

	/**
	 * Set the image height.
	 * 
	 * @param height
	 * @return Fluent API
	 */
	ImageManipulationParameters setHeight(Integer height);

	/**
	 * Return the crop x-axis start coordinate.
	 * 
	 * @return
	 */
	Integer getStartx();

	/**
	 * Set the crop x-axis start coordinate.
	 * 
	 * @param startx
	 * @return Fluent API
	 */
	ImageManipulationParameters setStartx(Integer startx);

	/**
	 * Return the crop y-axis start coordinate.
	 * 
	 * @return
	 */
	Integer getStarty();

	/**
	 * Set the crop y-axis start coordinate.
	 * 
	 * @param starty
	 * @return Fluent API
	 */
	ImageManipulationParameters setStarty(Integer starty);

	/**
	 * Return the crop height.
	 * 
	 * @return
	 */
	Integer getCroph();

	/**
	 * Set the crop height.
	 * 
	 * @param croph
	 * @return Fluent API
	 */
	ImageManipulationParameters setCroph(Integer croph);

	/**
	 * Return the crop width.
	 * 
	 * @return
	 */
	Integer getCropw();

	/**
	 * Set the crop width.
	 * 
	 * @param cropw
	 * @return Fluent API
	 */
	ImageManipulationParameters setCropw(Integer cropw);

	/**
	 * Check whether all required crop parameters have been set.
	 * 
	 * @param options
	 */
	void validateLimits(ImageManipulatorOptions options);

	/**
	 * Check whether all needed crop parameters have been set.
	 * @return
	 */
	boolean hasAllCropParameters();

	/**
	 * Validate the image crop parameters and check whether those would exceed the source image dimensions.
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 */
	void validateCropBounds(int imageWidth, int imageHeight);

	/**
	 * Generate cache key.
	 * 
	 * @return
	 */
	String getCacheKey();

	/**
	 * Check whether any of the parameters is set.
	 * 
	 * @return
	 */
	boolean isSet();

}

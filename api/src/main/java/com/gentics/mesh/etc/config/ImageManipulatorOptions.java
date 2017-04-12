package com.gentics.mesh.etc.config;

import java.io.File;

/**
 * Image manipulation options are used for image resize and image crop actions.
 */
public class ImageManipulatorOptions {

	private String imageCacheDirectory = "data" + File.separator + "binaryImageCache";

	private Integer maxWidth = 2048;
	private Integer maxHeight = 2048;

	/**
	 * Return the binary image cache directory.
	 * 
	 * @return
	 */
	public String getImageCacheDirectory() {
		return imageCacheDirectory;
	}

	/**
	 * Set the binary image cache directory.
	 * 
	 * @param imageCacheDirectory
	 * @return Fluent API
	 */
	public ImageManipulatorOptions setImageCacheDirectory(String imageCacheDirectory) {
		this.imageCacheDirectory = imageCacheDirectory;
		return this;
	}

	/**
	 * Return the maximum image height.
	 * 
	 * @return
	 */
	public Integer getMaxHeight() {
		return maxHeight;
	}

	/**
	 * Set the maximum image height.
	 * 
	 * @param maxHeight
	 * @return Fluent API
	 */
	public ImageManipulatorOptions setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		return this;
	}

	/**
	 * Return the maximum allowed image width.
	 * 
	 * @return
	 */
	public Integer getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Set the maximum allowed image width.
	 * 
	 * @param maxWidth
	 * @return Fluent API
	 */
	public ImageManipulatorOptions setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}
}

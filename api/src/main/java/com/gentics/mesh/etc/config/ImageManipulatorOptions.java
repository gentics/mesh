package com.gentics.mesh.etc.config;

/**
 * Image manipulation options are used for image resize and image crop actions.
 */
public class ImageManipulatorOptions {

	private String imageCacheDirectory = "binaryImageCache";

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
	 */
	public void setImageCacheDirectory(String imageCacheDirectory) {
		this.imageCacheDirectory = imageCacheDirectory;
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
	 */
	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
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
	 */
	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}
}

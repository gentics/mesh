package com.gentics.mesh.etc.config;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Image manipulation options are used for image resize and image crop actions.
 */
@GenerateDocumentation
public class ImageManipulatorOptions implements Option {

	private String imageCacheDirectory = "data" + File.separator + "binaryImageCache";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the maximum allowed image resize width. Resizing is a memory intensive operation and thus this limit can help avoid memory issues.")
	@EnvironmentVariable(name = "IMAGE_MAX_WIDTH", description = "Override the max width for image resize operations.")
	private Integer maxWidth = 2048;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the maximum allowed image resize height. Resizing is a memory intensive operation and thus this limit can help avoid memory issues.")
	@EnvironmentVariable(name = "IMAGE_MAX_HEIGHT", description = "Override the max height for image resize operations.")
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

	public void validate(MeshOptions meshOptions) {
		// TODO Auto-generated method stub

	}
}

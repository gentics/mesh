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

	public static final String MESH_IMAGE_MAX_WIDTH_ENV = "MESH_IMAGE_MAX_WIDTH";
	public static final String MESH_IMAGE_MAX_HEIGHT_ENV = "MESH_IMAGE_MAX_HEIGHT";
	public static final int DEFAULT_MAX_WIDTH = 2048;
	public static final int DEFAULT_MAX_HEIGHT = 2048;

	private String imageCacheDirectory = "data" + File.separator + "binaryImageCache";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the maximum allowed image resize width. Resizing is a memory intensive operation and thus this limit can help avoid memory issues. Default: "
		+ DEFAULT_MAX_WIDTH)
	@EnvironmentVariable(name = MESH_IMAGE_MAX_WIDTH_ENV, description = "Override the max width for image resize operations.")
	private Integer maxWidth = DEFAULT_MAX_WIDTH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the maximum allowed image resize height. Resizing is a memory intensive operation and thus this limit can help avoid memory issues. Default: "
		+ DEFAULT_MAX_HEIGHT)
	@EnvironmentVariable(name = MESH_IMAGE_MAX_HEIGHT_ENV, description = "Override the max height for image resize operations.")
	private Integer maxHeight = DEFAULT_MAX_HEIGHT;

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

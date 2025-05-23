package com.gentics.mesh.etc.config;

import java.io.File;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
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
	public static final String MESH_IMAGE_JPEG_QUALITY_ENV = "MESH_IMAGE_JPEG_QUALITY";
	public static final String MESH_IMAGE_RESAMPLE_FILTER_ENV = "MESH_IMAGE_RESAMPLE_FILTER";
	public static final String MESH_IMAGE_CACHE_DIRECTORY_ENV = "MESH_IMAGE_CACHE_DIRECTORY";
	public static final String MESH_IMAGE_CACHE_CLEAN_INTERVAL_ENV = "MESH_IMAGE_CACHE_CLEAN_INTERVAL";
	public static final String MESH_IMAGE_CACHE_MAX_IDLE_ENV = "MESH_IMAGE_CACHE_MAX_IDLE";
	public static final String MESH_IMAGE_CACHE_TOUCH_ENV = "MESH_IMAGE_CACHE_TOUCH";
	public static final String MESH_IMAGE_MANIPULATION_MODE_ENV = "MESH_IMAGE_MANIPULATION_MODE";

	public static final int DEFAULT_MAX_WIDTH = 2048;
	public static final int DEFAULT_MAX_HEIGHT = 2048;
	public static final float DEFAULT_JPEG_QUALITY = 0.95f;
	public static final String DEFAULT_IMAGE_CACHE_DIRECTORY = "data" + File.separator + "binaryImageCache";
	public static final String DEFAULT_IMAGE_CACHE_CLEAN_INTERVAL = "PT0S";
	public static final String DEFAULT_IMAGE_CACHE_MAX_IDLE = "PT1M";
	public static final boolean DEFAULT_IMAGE_CACHE_TOUCH = false;
	// This is the default filter in ImageMagick
	public static final ResampleFilter DEFAULT_RESAMPLE_FILTER = ResampleFilter.LANCZOS;
	public static final ImageManipulationMode DEFAULT_IMAGE_MANIPULATION_MODE = ImageManipulationMode.ON_DEMAND;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the image manipulation mode. Default: ON_DEMAND.")
	@EnvironmentVariable(name = MESH_IMAGE_MANIPULATION_MODE_ENV, description = "Override the image manipulation mode.")
	private ImageManipulationMode mode = DEFAULT_IMAGE_MANIPULATION_MODE;	

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the path for image cache directory. Default: data/binaryImageCache")
	@EnvironmentVariable(name = MESH_IMAGE_CACHE_DIRECTORY_ENV, description = "Override the path for image cache directory.")
	private String imageCacheDirectory = DEFAULT_IMAGE_CACHE_DIRECTORY;

	@JsonProperty(defaultValue = DEFAULT_IMAGE_CACHE_CLEAN_INTERVAL)
	@JsonPropertyDescription("Image cache clean interval in ISO 8601 duration format. Setting this to PT0S will disable the image cache cleanup. Default: " + DEFAULT_IMAGE_CACHE_CLEAN_INTERVAL)
	@EnvironmentVariable(name = MESH_IMAGE_CACHE_CLEAN_INTERVAL_ENV, description = "Overwrite the image cache clean interval.")
	private String imageCacheCleanInterval = DEFAULT_IMAGE_CACHE_CLEAN_INTERVAL;

	@JsonProperty(defaultValue = DEFAULT_IMAGE_CACHE_MAX_IDLE)
	@JsonPropertyDescription("Image cache maximum idle time in ISO 8601 duration format. Default: " + DEFAULT_IMAGE_CACHE_MAX_IDLE)
	@EnvironmentVariable(name = MESH_IMAGE_CACHE_CLEAN_INTERVAL_ENV, description = "Overwrite the image cache max idle time.")
	private String imageCacheMaxIdle = DEFAULT_IMAGE_CACHE_MAX_IDLE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Whether the Image cache files should be touched when accessed. Default: " + DEFAULT_IMAGE_CACHE_TOUCH)
	@EnvironmentVariable(name = MESH_IMAGE_CACHE_TOUCH_ENV, description = "Overwrite the image cache touch behaviour.")
	private boolean imageCacheTouch = DEFAULT_IMAGE_CACHE_TOUCH;

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

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the quality of the output of JPEG images. Must be a value between inclusive 0 and inclusive 1. Default: "
		+ DEFAULT_JPEG_QUALITY)
	@EnvironmentVariable(name = MESH_IMAGE_JPEG_QUALITY_ENV, description = "Override the JPEG quality for image resize operations.")
	private Float jpegQuality = DEFAULT_JPEG_QUALITY;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the filter that is used when resizing images. Default: LANCZOS")
	@EnvironmentVariable(name = MESH_IMAGE_RESAMPLE_FILTER_ENV, description = "Override the sample filter for image resize operations.")
	private ResampleFilter resampleFilter = DEFAULT_RESAMPLE_FILTER;

	public String getImageCacheDirectory() {
		return imageCacheDirectory;
	}

	@Setter
	public ImageManipulatorOptions setImageCacheDirectory(String imageCacheDirectory) {
		this.imageCacheDirectory = imageCacheDirectory;
		return this;
	}

	public String getImageCacheCleanInterval() {
		return imageCacheCleanInterval;
	}

	@Setter
	public ImageManipulatorOptions setImageCacheCleanInterval(String imageCacheCleanInterval) {
		this.imageCacheCleanInterval = imageCacheCleanInterval;
		return this;
	}

	public String getImageCacheMaxIdle() {
		return imageCacheMaxIdle;
	}

	@Setter
	public ImageManipulatorOptions setImageCacheMaxIdle(String imageCacheMaxIdle) {
		this.imageCacheMaxIdle = imageCacheMaxIdle;
		return this;
	}

	public boolean isImageCacheTouch() {
		return imageCacheTouch;
	}

	@Setter
	public ImageManipulatorOptions setImageCacheTouch(boolean imageCacheTouch) {
		this.imageCacheTouch = imageCacheTouch;
		return this;
	}

	public Integer getMaxHeight() {
		return maxHeight;
	}

	@Setter
	public ImageManipulatorOptions setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		return this;
	}

	public Integer getMaxWidth() {
		return maxWidth;
	}

	@Setter
	public ImageManipulatorOptions setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	public float getJpegQuality() {
		return jpegQuality;
	}

	@Setter
	public ImageManipulatorOptions setJpegQuality(float jpegQuality) {
		this.jpegQuality = jpegQuality;
		return this;
	}

	public ResampleFilter getResampleFilter() {
		return resampleFilter;
	}

	@Setter
	public ImageManipulatorOptions setResampleFilter(ResampleFilter resampleFilter) {
		this.resampleFilter = resampleFilter;
		return this;
	}

	public ImageManipulationMode getMode() {
		return mode;
	}

	@Setter
	public ImageManipulatorOptions setMode(ImageManipulationMode mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * Validate the options.
	 */
	public void validate(MeshOptions meshOptions) {
		try {
			Duration.parse(imageCacheCleanInterval);
		} catch (Exception e) {
			throw new IllegalArgumentException("imageCacheCleanInterval must be formatted in ISO 8601 duration format.");
		}
		try {
			Duration.parse(imageCacheMaxIdle);
		} catch (Exception e) {
			throw new IllegalArgumentException("imageCacheMaxIdle must be formatted in ISO 8601 duration format.");
		}
	}
}

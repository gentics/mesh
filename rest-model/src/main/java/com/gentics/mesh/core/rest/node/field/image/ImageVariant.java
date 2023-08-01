package com.gentics.mesh.core.rest.node.field.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Stored image manipulation variant.
 * 
 * @author plyhun
 *
 */
public class ImageVariant implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Image width.")
	private Integer width;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Image height.")
	private Integer height;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Image crop parameters.")
	private ImageRect rect;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image crop mode.")
	private CropMode cropMode;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image resize mode.")
	private ResizeMode resizeMode;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point, containing factors of the image width/height. The value 0.5 is the center of the image.")
	private FocalPoint focalPoint;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point zoom factor.")
	private Float focalZoom;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image file size.")
	private Long fileSize;

	@JsonPropertyDescription("This flag states that this variant is an original image.")
	private boolean origin = false;

	/**
	 * REST ctor.
	 */
	public ImageVariant() {}

	/**
	 * Parameter ctor.
	 * 
	 * @param width
	 * @param height
	 * @param rect
	 * @param cropMode
	 * @param resizeMode
	 * @param focalPoint
	 * @param focalZoom
	 */
	public ImageVariant(Integer width, Integer height, ImageRect rect, CropMode cropMode, ResizeMode resizeMode,
			FocalPoint focalPoint, Float focalZoom) {
		super();
		this.width = width;
		this.height = height;
		this.rect = rect;
		this.cropMode = cropMode;
		this.resizeMode = resizeMode;
		this.focalPoint = focalPoint;
		this.focalZoom = focalZoom;
	}

	public Integer getWidth() {
		return width;
	}

	public ImageVariant setWidth(Integer width) {
		this.width = width;
		return this;
	}

	public Integer getHeight() {
		return height;
	}

	public ImageVariant setHeight(Integer height) {
		this.height = height;
		return this;
	}

	public ImageRect getRect() {
		return rect;
	}

	public ImageVariant setRect(ImageRect rect) {
		this.rect = rect;
		return this;
	}

	public CropMode getCropMode() {
		return cropMode;
	}

	public ImageVariant setCropMode(CropMode cropMode) {
		this.cropMode = cropMode;
		return this;
	}

	public ResizeMode getResizeMode() {
		return resizeMode;
	}

	public ImageVariant setResizeMode(ResizeMode resizeMode) {
		this.resizeMode = resizeMode;
		return this;
	}

	public FocalPoint getFocalPoint() {
		return focalPoint;
	}

	public ImageVariant setFocalPoint(FocalPoint focalPoint) {
		this.focalPoint = focalPoint;
		return this;
	}

	public Float getFocalZoom() {
		return focalZoom;
	}

	public ImageVariant setFocalZoom(Float focalZoom) {
		this.focalZoom = focalZoom;
		return this;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public ImageVariant setFileSize(Long fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public boolean isOrigin() {
		return origin;
	}

	public ImageVariant setOrigin(boolean origin) {
		this.origin = origin;
		return this;
	}
}

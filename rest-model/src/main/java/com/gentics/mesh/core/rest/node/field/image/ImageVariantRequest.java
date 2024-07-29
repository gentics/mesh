package com.gentics.mesh.core.rest.node.field.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;

public class ImageVariantRequest implements ImageManipulation, RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Desired image width. Set to 'auto' to compute automatically from the image height and ratio.")
	private String width;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Desired image height. Set to 'auto' to compute automatically from the image width and ratio.")
	private String height;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Desired image crop parameters.")
	private ImageRect rect;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image crop mode.")
	private CropMode cropMode;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image resize mode.")
	private ResizeMode resizeMode = ResizeMode.SMART;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point, containing factors of the image width/height. The value 0.5 is the center of the image.")
	private FocalPoint focalPoint;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point zoom factor.")
	private Float focalZoom;

	/**
	 * REST ctor.
	 */
	public ImageVariantRequest() {
	}

	@Override
	public String getWidth() {
		return width;
	}

	@Override
	public ImageVariantRequest setWidth(String width) {
		this.width = width;
		return this;
	}

	@Override
	public ImageVariantRequest setWidth(Integer width) {
		this.width = width == null ? null : width.toString();
		return this;
	}

	@Override
	public String getHeight() {
		return height;
	}

	@Override
	public ImageVariantRequest setHeight(String height) {
		this.height = height;
		return this;
	}

	@Override
	public ImageVariantRequest setHeight(Integer height) {
		this.height = height == null ? null : height.toString();
		return this;
	}

	@Override
	public ImageRect getRect() {
		return rect;
	}

	@Override
	public ImageRect setRect(ImageRect rect) {
		this.rect = rect;
		return rect;
	}

	@Override
	public CropMode getCropMode() {
		return cropMode;
	}

	@Override
	public ImageVariantRequest setCropMode(CropMode mode) {
		this.cropMode = mode;
		return this;
	}

	@Override
	public ResizeMode getResizeMode() {
		return resizeMode;
	}

	@Override
	public ImageVariantRequest setResizeMode(ResizeMode mode) {
		this.resizeMode = mode;
		return this;
	}

	@Override
	public boolean hasFocalPoint() {
		return focalPoint != null;
	}

	@Override
	public FocalPoint getFocalPoint() {
		return focalPoint;
	}

	@Override
	public Float getFocalPointZoom() {
		return focalZoom;
	}

	@Override
	public ImageVariantRequest setFocalPoint(FocalPoint point) {
		this.focalPoint = point;
		return this;
	}

	@Override
	public ImageVariantRequest setFocalPoint(float x, float y) {
		ImageManipulation.super.setFocalPoint(x, y);
		return this;
	}

	@Override
	public ImageVariantRequest setFocalPointZoom(Float factor) {
		this.focalZoom = factor;
		return this;
	}

	@Override
	public ImageVariantRequest validateFocalPointParameter() {
		return this;
	}
}

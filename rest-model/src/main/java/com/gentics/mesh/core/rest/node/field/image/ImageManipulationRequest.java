package com.gentics.mesh.core.rest.node.field.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.parameter.image.ResizeMode;

public class ImageManipulationRequest implements ImageManipulation, RestModel {

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
	private ResizeMode resizeMode;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point, containing factors of the image width/height. The value 0.5 is the center of the image.")
	private FocalPoint focalPoint;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The image focal point zoom factor.")
	private Float focalZoom;

	@Override
	public String getWidth() {
		return width;
	}

	@Override
	public ImageManipulationRequest setWidth(String width) {
		this.width = width;
		return this;
	}

	@Override
	public ImageManipulationRequest setWidth(Integer width) {
		this.width = width == null ? null : width.toString();
		return null;
	}

	@Override
	public String getHeight() {
		return height;
	}

	@Override
	public ImageManipulationRequest setHeight(String height) {
		this.height = height;
		return this;
	}

	@Override
	public ImageManipulationRequest setHeight(Integer height) {
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
	public ImageManipulationRequest setCropMode(CropMode mode) {
		this.cropMode = mode;
		return this;
	}

	@Override
	public ResizeMode getResizeMode() {
		return resizeMode;
	}

	@Override
	public ImageManipulationRequest setResizeMode(ResizeMode mode) {
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
	public ImageManipulationRequest setFocalPoint(FocalPoint point) {
		this.focalPoint = point;
		return this;
	}

	@Override
	public ImageManipulationRequest setFocalPoint(float x, float y) {
		ImageManipulation.super.setFocalPoint(x, y);
		return this;
	}

	@Override
	public ImageManipulationRequest setFocalPointZoom(Float factor) {
		this.focalZoom = factor;
		return this;
	}

	@Override
	public ImageManipulationRequest validateFocalPointParameter() {
		return this;
	}
}

package com.gentics.mesh.core.rest.node.field.image;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ImageVariantResponse implements RestModel {

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

	@JsonPropertyDescription("This flag states that this variant is a proportional (aka 'auto') image.")
	private boolean auto = false;

	/**
	 * REST ctor.
	 */
	public ImageVariantResponse() {}

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
	public ImageVariantResponse(Integer width, Integer height, ImageRect rect, CropMode cropMode, ResizeMode resizeMode,
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

	public ImageVariantResponse setWidth(Integer width) {
		this.width = width;
		return this;
	}

	public Integer getHeight() {
		return height;
	}

	public ImageVariantResponse setHeight(Integer height) {
		this.height = height;
		return this;
	}

	public ImageRect getRect() {
		return rect;
	}

	public ImageVariantResponse setRect(ImageRect rect) {
		this.rect = rect;
		return this;
	}

	public CropMode getCropMode() {
		return cropMode;
	}

	public ImageVariantResponse setCropMode(CropMode cropMode) {
		this.cropMode = cropMode;
		return this;
	}

	public ResizeMode getResizeMode() {
		return resizeMode;
	}

	public ImageVariantResponse setResizeMode(ResizeMode resizeMode) {
		this.resizeMode = resizeMode;
		return this;
	}

	public FocalPoint getFocalPoint() {
		return focalPoint;
	}

	public ImageVariantResponse setFocalPoint(FocalPoint focalPoint) {
		this.focalPoint = focalPoint;
		return this;
	}

	public Float getFocalZoom() {
		return focalZoom;
	}

	public ImageVariantResponse setFocalZoom(Float focalZoom) {
		this.focalZoom = focalZoom;
		return this;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public ImageVariantResponse setFileSize(Long fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public boolean isOrigin() {
		return origin;
	}

	public ImageVariantResponse setOrigin(boolean origin) {
		this.origin = origin;
		return this;
	}

	public boolean isAuto() {
		return auto;
	}

	public ImageVariantResponse setAuto(boolean auto) {
		this.auto = auto;
		return this;
	}

	@Override
	@JsonIgnore
	public int hashCode() {
		return Objects.hash(auto, cropMode, /*fileSize, */focalPoint, focalZoom, height, origin, rect, resizeMode, width);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageVariantResponse other = (ImageVariantResponse) obj;
		return auto == other.auto && cropMode == other.cropMode /*&& Objects.equals(fileSize, other.fileSize)*/
				&& Objects.equals(focalPoint, other.focalPoint) && Objects.equals(focalZoom, other.focalZoom)
				&& Objects.equals(height, other.height) && origin == other.origin && Objects.equals(rect, other.rect)
				&& resizeMode == other.resizeMode && Objects.equals(width, other.width);
	}

	/**
	 * Create a {@link ImageVariantRequest} out of this response.
	 * 
	 * @return
	 */
	public ImageVariantRequest toRequest() {
		return toRequest(true);
	}

	/**
	 * Create a {@link ImageVariantRequest} out of this response.
	 * @param autoKeepWidth if `auto` is set, and this param is true, the width value is kept, replacing height with `auto`, otherwise the height value is kept.
	 * @return
	 */
	public ImageVariantRequest toRequest(boolean autoKeepWidth) {
		ImageVariantRequest request = new ImageVariantRequest()
				.setWidth((isAuto() && getWidth() == null) ? "auto" : (getWidth() == null ? null : Integer.toString(getWidth())))
				.setHeight((isAuto() && getHeight() == null) ? "auto" : (getHeight() == null ? null : Integer.toString(getHeight())))
				.setCropMode(getCropMode())
				.setFocalPoint(getFocalPoint())
				.setFocalPointZoom(getFocalZoom())
				.setResizeMode(getResizeMode());
		if (getRect() != null) {
			request.setRect(getRect().getStartX(), getRect().getStartY(), getRect().getHeight(), getRect().getWidth());
		}
		if (isAuto() && getWidth() != null && getHeight() != null) {
			if (autoKeepWidth) {
				request.setHeight("auto");
			} else {
				request.setWidth("auto");
			}
		}
		return request;
	}

	@Override
	public String toString() {
		return toJson();
	}
}

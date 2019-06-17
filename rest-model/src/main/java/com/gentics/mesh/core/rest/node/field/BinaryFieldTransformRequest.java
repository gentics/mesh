package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.parameter.image.ImageRect;

/**
 * POJO for a binary field transform request
 */
public class BinaryFieldTransformRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version number which must be provided in order to handle and detect concurrent changes to the node content.")
	private String version;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO 639-1 language tag of the node which provides the image which should be transformed.")
	private String language;

	@JsonPropertyDescription("New width of the image.")
	private Integer width;

	@JsonPropertyDescription("New height of the image.")
	private Integer height;

	@JsonPropertyDescription("Crop area.")
	private ImageRect cropRect;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional new focal point for the transformed image.")
	private FocalPoint focalPoint;

	/**
	 * Get resize width
	 * 
	 * @return resize width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the resize width
	 * 
	 * @param width
	 *            resize width
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setWidth(Integer width) {
		this.width = width;
		return this;
	}

	/**
	 * Get the resize height
	 * 
	 * @return resize height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the resize height
	 * 
	 * @param height
	 *            resize height
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setHeight(Integer height) {
		this.height = height;
		return this;
	}

	/**
	 * Return the crop area.
	 * 
	 * @return
	 */
	public ImageRect getCropRect() {
		return cropRect;
	}

	/**
	 * Set the crop area.
	 */
	public BinaryFieldTransformRequest setCropRect(ImageRect rect) {
		this.cropRect = rect;
		return this;
	}

	/**
	 * Set the crop area.
	 * 
	 * @param startX
	 * @param startY
	 * @param height
	 * @param width
	 * @return
	 */
	public BinaryFieldTransformRequest setCropRect(int startX, int startY, int height, int width) {
		return setCropRect(new ImageRect(startX, startY, height, width));
	}

	/**
	 * Return the node language.
	 * 
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node content which contains the image.
	 * 
	 * @param language
	 * @return Fluent API
	 */
	public BinaryFieldTransformRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * Return the node version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the node version.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	public BinaryFieldTransformRequest setVersion(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Return the focal point.
	 * 
	 * @return
	 */
	public FocalPoint getFocalPoint() {
		return focalPoint;
	}

	/**
	 * Set the focal point.
	 * 
	 * @param focalPoint
	 * @return Fluent API
	 */
	public BinaryFieldTransformRequest setFocalPoint(FocalPoint focalPoint) {
		this.focalPoint = focalPoint;
		return this;
	}
}

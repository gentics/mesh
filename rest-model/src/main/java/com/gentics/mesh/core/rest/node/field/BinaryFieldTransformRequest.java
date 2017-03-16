package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.VersionReference;

/**
 * POJO for a binary field transform request
 */
public class BinaryFieldTransformRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version reference which must be provided in order to handle and detect concurrent changes to the node content.")
	private VersionReference version;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO 639-1 language tag of the node which provides the image which should be transformed.")
	private String language;

	@JsonPropertyDescription("New width of the image.")
	private Integer width;

	@JsonPropertyDescription("New height of the image.")
	private Integer height;

	@JsonPropertyDescription("Crop x axis start coordinate.")
	private Integer cropx;

	@JsonPropertyDescription("Crop y axis start coordinate.")
	private Integer cropy;

	@JsonPropertyDescription("Crop area height.")
	private Integer croph;

	@JsonPropertyDescription("Crop area width.")
	private Integer cropw;

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
	 * Get the crop x-axis start coordinate.
	 * 
	 * @return crop x-axis start coordinate
	 */
	public Integer getCropx() {
		return cropx;
	}

	/**
	 * Set the crop x-axis start coordinate.
	 * 
	 * @param cropx
	 *            crop x-axis start coordinate
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropx(Integer cropx) {
		this.cropx = cropx;
		return this;
	}

	/**
	 * Get the crop y-axis start coordinate.
	 * 
	 * @return crop y-axis start coordinate
	 */
	public Integer getCropy() {
		return cropy;
	}

	/**
	 * Set the crop y-axis start coordinate.
	 * 
	 * @param cropy
	 *            crop y-axis start coordinate
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropy(Integer cropy) {
		this.cropy = cropy;
		return this;
	}

	/**
	 * Get the crop height.
	 * 
	 * @return crop height
	 */
	public Integer getCroph() {
		return croph;
	}

	/**
	 * Set the crop height.
	 * 
	 * @param croph
	 *            crop height
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCroph(Integer croph) {
		this.croph = croph;
		return this;
	}

	/**
	 * Get the crop width.
	 * 
	 * @return crop width
	 */
	public Integer getCropw() {
		return cropw;
	}

	/**
	 * Set the crop width.
	 * 
	 * @param cropw
	 *            crop width
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropw(Integer cropw) {
		this.cropw = cropw;
		return this;
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
	public VersionReference getVersion() {
		return version;
	}

	/**
	 * Set the node version.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	public BinaryFieldTransformRequest setVersion(VersionReference version) {
		this.version = version;
		return this;
	}
}

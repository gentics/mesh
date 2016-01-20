package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a binary field transform request
 */
public class BinaryFieldTransformRequest implements RestModel {
	private Integer width;
	private Integer height;
	private Integer cropx;
	private Integer cropy;
	private Integer croph;
	private Integer cropw;

	/**
	 * Get resize width
	 * @return resize width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the resize width
	 * @param width resize width
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setWidth(Integer width) {
		this.width = width;
		return this;
	}

	/**
	 * Get the resize height
	 * @return resize height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the resize height
	 * @param height resize height
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setHeight(Integer height) {
		this.height = height;
		return this;
	}

	/**
	 * Get the crop x-axis start coordinate.
	 * @return crop x-axis start coordinate 
	 */
	public Integer getCropx() {
		return cropx;
	}

	/**
	 * Set the crop x-axis start coordinate.
	 * @param cropx crop x-axis start coordinate
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropx(Integer cropx) {
		this.cropx = cropx;
		return this;
	}

	/**
	 * Get the crop y-axis start coordinate.
	 * @return crop y-axis start coordinate 
	 */
	public Integer getCropy() {
		return cropy;
	}

	/**
	 * Set the crop y-axis start coordinate.
	 * @param cropy crop y-axis start coordinate
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropy(Integer cropy) {
		this.cropy = cropy;
		return this;
	}

	/**
	 * Get the crop height.
	 * @return crop height
	 */
	public Integer getCroph() {
		return croph;
	}

	/**
	 * Set the crop height.
	 * @param croph crop height
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCroph(Integer croph) {
		this.croph = croph;
		return this;
	}

	/**
	 * Get the crop width.
	 * @return crop width
	 */
	public Integer getCropw() {
		return cropw;
	}

	/**
	 * Set the crop width.
	 * @param cropw crop width
	 * @return fluent API
	 */
	public BinaryFieldTransformRequest setCropw(Integer cropw) {
		this.cropw = cropw;
		return this;
	}
}

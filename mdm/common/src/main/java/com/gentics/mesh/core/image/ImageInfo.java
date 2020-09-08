package com.gentics.mesh.core.image;

/**
 * Container object which holds image information.
 */
public class ImageInfo {

	private Integer width;

	private Integer height;

	private String dominantColor;

	public Integer getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getDominantColor() {
		return dominantColor;
	}

	public void setDominantColor(String dominantColor) {
		this.dominantColor = dominantColor;
	}

	@Override
	public String toString() {
		return "width:" + width + " height:" + height + " color: " + dominantColor;
	}

}

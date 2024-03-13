package com.gentics.mesh.parameter.image;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Class which represents an image rectangular.
 */
public class ImageRect {

	private int startX;
	private int startY;

	private int width;
	private int height;

	/**
	 * Create a new image rect by parsing the given rect string which contains the rectengular dimensions in the format: x,y,w,h
	 * 
	 * @param dimensions
	 */
	public ImageRect(String dimensions) {
		if (dimensions == null) {
			throw error(BAD_REQUEST, "image_error_incomplete_crop_parameters");
		}
		String[] parts = dimensions.split(",");
		if (parts.length != 4) {
			throw error(BAD_REQUEST, "image_error_incomplete_crop_parameters");
		}
		this.startX = Integer.parseInt(parts[0]);
		this.startY = Integer.parseInt(parts[1]);
		this.width = Integer.parseInt(parts[2]);
		this.height = Integer.parseInt(parts[3]);
	}

	public ImageRect(int startX, int startY, int height, int width) {
		this.startX = startX;
		this.startY = startY;
		this.height = height;
		this.width = width;
	}

	public ImageRect() {
	}

	public int getStartX() {
		return startX;
	}

	public int getStartY() {
		return startY;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return getStartX() + "," + getStartY() + "," + getWidth() + "," + getHeight();
	}

	/**
	 * Validate the image crop parameters and check whether those would exceed the source image dimensions.
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 */
	public void validateCropBounds(int imageWidth, int imageHeight) {
		if (getStartX() + getWidth() > imageWidth || getStartY() + getHeight() > imageHeight) {
			throw error(BAD_REQUEST, "image_error_crop_out_of_bounds", String.valueOf(imageWidth), String.valueOf(imageHeight));
		}
	}

	/**
	 * Validate the image crop area parameters.
	 */
	public void validate() {
		Integer croph = getHeight();
		Integer cropw = getWidth();
		Integer startx = getStartX();
		Integer starty = getStartY();
		// Check whether all required crop parameters have been set when at least one crop parameter has been set.
		boolean hasOneCropParameter = croph != null || cropw != null || startx != null || starty != null;
		if (hasOneCropParameter) {
			if (croph != null && croph <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, String.valueOf(croph));
			}

			if (cropw != null && cropw <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, String.valueOf(cropw));
			}

			if (startx != null && startx <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, String.valueOf(
					startx));
			}

			if (starty != null && starty <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParameters.RECT_QUERY_PARAM_KEY, String.valueOf(
					starty));
			}
		}
	}

	@Override
	@JsonIgnore
	public int hashCode() {
		return Objects.hash(height, startX, startY, width);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageRect other = (ImageRect) obj;
		return height == other.height && startX == other.startX && startY == other.startY && width == other.width;
	}
}

package com.gentics.mesh.core.rest.node.field.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Focal point for which is the position is described by using factors of the height and width of the image instead of absolute pixel values.
 */
public class FocalPoint implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("The horizontal position of the focal point. The value is a factor of the image width. The value 0.5 is the center of the image.")
	private float x;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The vertical position of the focal point. The value is a factor of the image height. The value 0.5 is the center of the image.")
	private float y;

	public FocalPoint() {
	}

	/**
	 * Construct a new focal point.
	 * 
	 * @param x
	 * @param y
	 */
	public FocalPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FocalPoint) {
			FocalPoint point = (FocalPoint) obj;
			return point.x == x && point.y == y;
		} else {
			return super.equals(obj);
		}
	}

	@JsonIgnore
	public Point convertToAbsolutePoint(Point imageSize) {
		// Recalculate the focal point position since the image has been resized
		int fpx = (int) (getX() * imageSize.getX());
		int fpy = (int) (getY() * imageSize.getY());

		// clamp to 1,1
		if (fpx == 0) {
			fpx = 1;
		}
		if (fpy == 0) {
			fpy = 1;
		}
		return new Point(fpx, fpy);
	}

	@Override
	public String toString() {
		return x + "-" + y;
	}

}

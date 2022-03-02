package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.node.field.image.Point;

/**
 * A binary image element.
 * 
 * @author plyhun
 *
 */
public interface HibImageDataElement extends HibBinaryDataElement {

	/**
	 * Return the image height of the binary
	 * 
	 * @return Image height or null when the height could not be determined
	 */
	Integer getImageHeight();

	/**
	 * Set the image height.
	 * 
	 * @param height
	 * @return Fluent API
	 */
	HibImageDataElement setImageHeight(Integer height);

	/**
	 * Return the image width of the binary
	 * 
	 * @return Image width or null when the width could not be determined
	 */
	Integer getImageWidth();

	/**
	 * Set the image width.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	HibImageDataElement setImageWidth(Integer width);

	/**
	 * Return the image size
	 * 
	 * @return Image size or null when the information could not be determined
	 */
	default Point getImageSize() {
		Integer x = getImageHeight();
		Integer y = getImageWidth();
		if (x == null || y == null) {
			return null;
		} else {
			return new Point(x, y);
		}
	}
}

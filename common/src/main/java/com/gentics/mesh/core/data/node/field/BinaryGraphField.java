package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;

/**
 * The BinaryField Domain Model interface. The field is an edge between the field container and the {@link Binary}
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField>, MeshEdge, DisplayField {

	static final String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	static final String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	static final String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	static final String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	static final String BINARY_IMAGE_FOCAL_POINT_X = "binaryImageFocalPointX";

	static final String BINARY_IMAGE_FOCAL_POINT_Y = "binaryImageFocalPointY";

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	default String getFileName() {
		return getProperty(BINARY_FILENAME_PROPERTY_KEY);
	}

	/**
	 * Copy the values of this field to the specified target field.
	 * 
	 * @param target
	 * @return Fluent API
	 */
	BinaryGraphField copyTo(BinaryGraphField target);

	/**
	 * Set the binary filename.
	 * 
	 * @param fileName
	 * @return Fluent API
	 */
	default BinaryGraphField setFileName(String fileName) {
		setProperty(BINARY_FILENAME_PROPERTY_KEY, fileName);
		return this;
	}

	/**
	 * Return the binary mime type of the node.
	 * 
	 * @return
	 */
	default String getMimeType() {
		return getProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	default BinaryGraphField setMimeType(String mimeType) {
		setProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY, mimeType);
		return this;
	}

	/**
	 * Check whether the binary data represents an image.
	 * 
	 * @return
	 */
	boolean hasImage();

	/**
	 * Set the binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	default BinaryGraphField setImageDominantColor(String dominantColor) {
		setProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		return this;
	}

	/**
	 * Return the binary image dominant color.
	 * 
	 * @return
	 */
	default String getImageDominantColor() {
		return getProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
	}

	/**
	 * Return the stored focal point of the image.
	 * 
	 * @return Focal point or null if no focal point has been set
	 */
	default FocalPoint getImageFocalPoint() {
		Float x = getProperty(BINARY_IMAGE_FOCAL_POINT_X);
		Float y = getProperty(BINARY_IMAGE_FOCAL_POINT_Y);
		if (x == null || y == null) {
			return null;
		}
		return new FocalPoint(x, y);
	}

	/**
	 * Set the image focal point.
	 * 
	 * @param point
	 */
	default void setImageFocalPoint(FocalPoint point) {
		setProperty(BINARY_IMAGE_FOCAL_POINT_X, point.getX());
		setProperty(BINARY_IMAGE_FOCAL_POINT_Y, point.getY());
	}

	/**
	 * Return the uuid of the binary field.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid of the binary field.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Return the referenced binary.
	 * 
	 * @return
	 */
	Binary getBinary();

}

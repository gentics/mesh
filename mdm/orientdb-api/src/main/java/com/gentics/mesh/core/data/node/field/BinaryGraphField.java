package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;

/**
 * The BinaryField Domain Model interface. The field is an edge between the field container and the {@link Binary}
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField>, MeshEdge, DisplayField, HibBinaryField {

	String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	String BINARY_IMAGE_FOCAL_POINT_X = "binaryImageFocalPointX";

	String BINARY_IMAGE_FOCAL_POINT_Y = "binaryImageFocalPointY";

	String META_DATA_PROPERTY_PREFIX = "metadata_";

	String BINARY_LAT_KEY = "metadata-lat";

	String BINARY_LON_KEY = "metadata-lon";

	String BINARY_ALT_KEY = "metadata-alt";

	String PLAIN_TEXT_KEY = "plainText"; 

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	default String getFileName() {
		return property(BINARY_FILENAME_PROPERTY_KEY);
	}

	/**
	 * Set the binary filename.
	 * 
	 * @param fileName
	 * @return Fluent API
	 */
	default HibBinaryField setFileName(String fileName) {
		property(BINARY_FILENAME_PROPERTY_KEY, fileName);
		return this;
	}

	/**
	 * Return the binary mime type of the node.
	 * 
	 * @return
	 */
	default String getMimeType() {
		return property(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	default HibBinaryField setMimeType(String mimeType) {
		property(BINARY_CONTENT_TYPE_PROPERTY_KEY, mimeType);
		return this;
	}

	/**
	 * Set the binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	default HibBinaryField setImageDominantColor(String dominantColor) {
		property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		return this;
	}

	/**
	 * Return the binary image dominant color.
	 * 
	 * @return
	 */
	default String getImageDominantColor() {
		return property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
	}

	/**
	 * Return the stored focal point of the image.
	 * 
	 * @return Focal point or null if no focal point has been set
	 */
	default FocalPoint getImageFocalPoint() {
		Float x = property(BINARY_IMAGE_FOCAL_POINT_X);
		Float y = property(BINARY_IMAGE_FOCAL_POINT_Y);
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
		property(BINARY_IMAGE_FOCAL_POINT_X, point.getX());
		property(BINARY_IMAGE_FOCAL_POINT_Y, point.getY());
	}

	/**
	 * Return the location latitude.
	 * 
	 * @return
	 */
	default Double getLocationLatitude() {
		return property(BINARY_LAT_KEY);
	}

	/**
	 * Set the location latitude.
	 * 
	 * @param lat
	 */
	default void setLocationLatitude(Double lat) {
		property(BINARY_LAT_KEY, lat);
	}

	/**
	 * Return the location longitude.
	 * 
	 * @return
	 */
	default Double getLocationLongitude() {
		return property(BINARY_LON_KEY);
	}

	/**
	 * Set the location longitude.
	 * 
	 * @param lon
	 */
	default void setLocationLongitude(Double lon) {
		property(BINARY_LON_KEY, lon);
	}

	/**
	 * Return the location altitude.
	 * 
	 * @return
	 */
	default Integer getLocationAltitude() {
		return property(BINARY_ALT_KEY);
	}

	/**
	 * Set the location altitude.
	 * 
	 * @param alt
	 */
	default void setLocationAltitude(Integer alt) {
		property(BINARY_ALT_KEY, alt);
	}

	/**
	 * Clear the metadata properties.
	 */
	default void clearMetadata() {
		setLocationAltitude(null);
		setLocationLongitude(null);
		setLocationLatitude(null);

		// Remove all other metadata properties
		getPropertyKeys().stream()
			.filter(e -> e.startsWith(META_DATA_PROPERTY_PREFIX))
			.forEach(e -> {
				setMetadata(e.substring(META_DATA_PROPERTY_PREFIX.length()), null);
			});
	}
}

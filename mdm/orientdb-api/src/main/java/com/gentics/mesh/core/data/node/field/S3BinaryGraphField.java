package com.gentics.mesh.core.data.node.field;

import java.util.Objects;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;

/**
 * The S3BinaryField Domain Model interface. The field is an edge between the field container and the {@link S3Binary}
 */
public interface S3BinaryGraphField extends BasicGraphField<S3BinaryField>, MeshEdge, DisplayField, S3HibBinaryField, GraphDeletableField {

	String S3_BINARY_FILENAME_PROPERTY_KEY = "fileName";

	String S3_BINARY_CONTENT_TYPE_PROPERTY_KEY = "s3binaryContentType";

	String S3_BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "s3binaryImageDominantColor";

	String S3_BINARY_IMAGE_FOCAL_POINT_X = "s3binaryImageFocalPointX";

	String S3_BINARY_IMAGE_FOCAL_POINT_Y = "s3binaryImageFocalPointY";

	String META_DATA_PROPERTY_PREFIX = "metadata_";

	String S3_BINARY_LAT_KEY = "metadata-lat";

	String S3_BINARY_LON_KEY = "metadata-lon";

	String S3_BINARY_ALT_KEY = "metadata-alt";

	String PLAIN_TEXT_KEY = "plainText";

	String AWS_S3_OBJECT_KEY = "s3ObjectKey";

	String S3_FILE_SIZE = "s3FileSize";

	/**
	 * Return the S3 binary filename.
	 * 
	 * @return
	 */
	default String getFileName() {
		return property(S3_BINARY_FILENAME_PROPERTY_KEY);
	}

	/**
	 * Set the S3 binary filename.
	 * 
	 * @param fileName
	 * @return Fluent API
	 */
	default S3HibBinaryField setFileName(String fileName) {
		property(S3_BINARY_FILENAME_PROPERTY_KEY, fileName);
		return this;
	}

	/**
	 * Return the S3 binary mime type of the node.
	 * 
	 * @return
	 */
	default String getMimeType() {
		return property(S3_BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	/**
	 * Set the S3 binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	default S3BinaryGraphField setMimeType(String mimeType) {
		property(S3_BINARY_CONTENT_TYPE_PROPERTY_KEY, mimeType);
		return this;
	}

	/**
	 * Set the S3 binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	default S3BinaryGraphField setImageDominantColor(String dominantColor) {
		property(S3_BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		return this;
	}

	/**
	 * Return the S3 binary image dominant color.
	 * 
	 * @return
	 */
	default String getImageDominantColor() {
		return property(S3_BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
	}

	/**
	 * Return the stored focal point of the image.
	 * 
	 * @return Focal point or null if no focal point has been set
	 */
	default FocalPoint getImageFocalPoint() {
		Float x = property(S3_BINARY_IMAGE_FOCAL_POINT_X);
		Float y = property(S3_BINARY_IMAGE_FOCAL_POINT_Y);
		if (x == null || y == null) {
			return null;
		}
		return new FocalPoint(x, y);
	}

	@Override
	default S3BinaryGraphField setImageFocalPoint(FocalPoint point) {
		property(S3_BINARY_IMAGE_FOCAL_POINT_X, point.getX());
		property(S3_BINARY_IMAGE_FOCAL_POINT_Y, point.getY());
		return this;
	}

	/**
	 * Set the location information.
	 * 
	 * @param loc
	 */
	default void setLocation(Location loc) {
		Objects.requireNonNull(loc, "A valid location object needs to be supplied. Got null.");
		setLocationLatitude(loc.getLat());
		setLocationLongitude(loc.getLon());
		Integer alt = loc.getAlt();
		if (alt != null) {
			setLocationAltitude(alt);
		}
	}

	/**
	 * Return the location latitude.
	 * 
	 * @return
	 */
	default Double getLocationLatitude() {
		return property(S3_BINARY_LAT_KEY);
	}

	/**
	 * Set the location latitude.
	 * 
	 * @param lat
	 */
	default void setLocationLatitude(Double lat) {
		property(S3_BINARY_LAT_KEY, lat);
	}

	/**
	 * Return the location longitude.
	 * 
	 * @return
	 */
	default Double getLocationLongitude() {
		return property(S3_BINARY_LON_KEY);
	}

	/**
	 * Set the location longitude.
	 * 
	 * @param lon
	 */
	default void setLocationLongitude(Double lon) {
		property(S3_BINARY_LON_KEY, lon);
	}

	/**
	 * Return the location altitude.
	 * 
	 * @return
	 */
	default Integer getLocationAltitude() {
		return property(S3_BINARY_ALT_KEY);
	}

	/**
	 * Set the location altitude.
	 * 
	 * @param alt
	 */
	default void setLocationAltitude(Integer alt) {
		property(S3_BINARY_ALT_KEY, alt);
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

	/**
	 * Return the S3 Object Key that serves as reference to AWS
	 *
	 * @return
	 */
	default String getS3ObjectKey() {
		return property(AWS_S3_OBJECT_KEY);
	}

	/**
	 * Set the S3 Object key
	 *
	 * @param key
	 */
	default void setS3ObjectKey(String key) {
		property(AWS_S3_OBJECT_KEY, key);
	}
	/**
	 * Return the S3 file size
	 *
	 * @return
	 */
	default Long getFileSize() {
		return property(S3_FILE_SIZE);
	}

	/**
	 * Set the S3 file size
	 *
	 * @param fileSize
	 */
	default void setFileSize(Long fileSize) {
		property(S3_FILE_SIZE, fileSize);
	}

}

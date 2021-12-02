package com.gentics.mesh.core.data.s3binary;


import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.node.field.HibBasicField;
import com.gentics.mesh.core.data.node.field.HibDisplayField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;
import com.gentics.mesh.util.UniquenessUtil;

/**
 * MDM interface for the s3binary field information.
 */
public interface S3HibBinaryField extends HibField, HibBasicField<S3BinaryField>, HibElement, HibDisplayField {

	/**
	 * Return the referenced s3binary entity.
	 * 
	 * @return
	 */
	S3HibBinary getS3Binary();

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	S3BinaryMetadata getMetadata();

	/**
	 * Return the filename of the s3binary in this field.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Return the mimetype of the s3binary.
	 * 
	 * @return
	 */
	String getMimeType();

	/**
	 * Set the binary mime type of the node.
	 *
	 * @param mimeType
	 * @return Fluent API
	 */
	S3HibBinaryField setMimeType(String mimeType);

	/**
	 * Check whether the binary data represents an image.
	 *
	 * @return
	 */
	boolean hasProcessableImage();

	/**
	 * Set the binary image dominant color.
	 *
	 * @param dominantColor
	 * @return Fluent API
	 */
	S3HibBinaryField setImageDominantColor(String dominantColor);

	/**
	 * Return the binary image dominant color.
	 *
	 * @return
	 */
	String getImageDominantColor();

	/**
	 * Return the stored focal point of the image.
	 *
	 * @return Focal point or null if no focal point has been set
	 */
	FocalPoint getImageFocalPoint();

	/**
	 * Set the image focal point.
	 *
	 * @param point
	 */
	void setImageFocalPoint(FocalPoint point);

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
	 * Set the metadata property.
	 *
	 * @param key
	 * @param value
	 */
	void setMetadata(String key, String value);

	/**
	 * Return the metadata properties.
	 *
	 * @return
	 */
	Map<String, String> getMetadataProperties();

	/**
	 * Return the location latitude.
	 *
	 * @return
	 */
	Double getLocationLatitude();

	/**
	 * Set the location latitude.
	 *
	 * @param lat
	 */
	void setLocationLatitude(Double lat);

	/**
	 * Return the location longitude.
	 *
	 * @return
	 */
	Double getLocationLongitude();

	/**
	 * Set the location longitude.
	 *
	 * @param lon
	 */
	void setLocationLongitude(Double lon);

	/**
	 * Return the location altitude.
	 *
	 * @return
	 */
	Integer getLocationAltitude();

	/**
	 * Set the location altitude.
	 *
	 * @param alt
	 */
	void setLocationAltitude(Integer alt);

	/**
	 * Clear the metadata properties.
	 */
	void clearMetadata();

	/**
	 * Set the plain text content.
	 * @param text
	 */
	void setPlainText(String text);

	/**
	 * Return the extracted plain text content of the binary.
	 * @return
	 */
	String getPlainText();

	/**
	 * Set the S3 Object key
	 * @param key
	 */
	void setS3ObjectKey(String key);

	/**
	 * Set name of the S3 file
	 * @param fileName
	 */
	S3HibBinaryField setFileName(String fileName);

	S3HibBinaryField copyTo(S3HibBinaryField field);

	/**
	 * Set the s3 file size
	 * @param size
	 */
    void setFileSize(Long size);
   
    /**
	 * Return the S3 Object Key that serves as reference to AWS
	 *
	 * @return
	 */
	String getS3ObjectKey();

	/**
	 * Return the S3 file size
	 *
	 * @return
	 */
	Long getFileSize();

	/**
	 * Increment any found postfix number in the filename.
	 * 
	 * e.g:
	 * <ul>
	 * <li>test.txt -> test_1.txt</li>
	 * <li>test -> test_1</li>
	 * <li>test.blub.txt -> test.blub_1.txt</li>
	 * <ul>
	 * 
	 */
	default void postfixFileName() {
		String oldName = getFileName();
		if (oldName != null && !oldName.isEmpty()) {
			setFileName(UniquenessUtil.suggestNewName(oldName));
		}
	}
}

package com.gentics.mesh.core.data.binary;

import java.util.Map;

import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;

/**
 * MDM interface for the binary field information.
 */
public interface HibBinaryField {

	/**
	 * Return the referenced binary entity.
	 * 
	 * @return
	 */
	HibBinary getBinary();

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	BinaryMetadata getMetadata();

	/**
	 * Return the filename of the binary in this field.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Return the mimetype of the binary.
	 * 
	 * @return
	 */
	String getMimeType();

	/**
	 * Copy the values of this field to the specified target field.
	 * 
	 * @param target
	 * @return Fluent API
	 */
	HibBinaryField copyTo(HibBinaryField target);

	/**
	 * Set the binary filename.
	 * 
	 * @param fileName
	 * @return Fluent API
	 */
	HibBinaryField setFileName(String fileName);

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
	void postfixFileName();

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	HibBinaryField setMimeType(String mimeType);

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
	HibBinaryField setImageDominantColor(String dominantColor);

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
	 * Set the location information.
	 * 
	 * @param loc
	 */
	void setLocation(Location loc);

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
}

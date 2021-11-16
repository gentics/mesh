package com.gentics.mesh.core.data.node.field;

import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.util.UniquenessUtil;

public interface HibBinaryField extends HibField, HibBasicField<BinaryField>, HibElement {

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getFileName();

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
	default void postfixFileName() {
		String oldName = getFileName();
		if (oldName != null && !oldName.isEmpty()) {
			setFileName(UniquenessUtil.suggestNewName(oldName));
		}

	}

	/**
	 * Return the binary mime type of the node.
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
	 * Return the referenced binary.
	 * 
	 * @return
	 */
	HibBinary getBinary();

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
	 * Return the {@link BinaryMetadata} REST model of the field.
	 * 
	 * @return
	 */
	BinaryMetadata getMetadata();

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

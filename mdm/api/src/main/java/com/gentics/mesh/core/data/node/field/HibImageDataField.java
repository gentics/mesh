package com.gentics.mesh.core.data.node.field;

import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.util.NodeUtil;

/**
 * Generic graphic image data field.
 * 
 * @author plyhun
 *
 */
public interface HibImageDataField extends HibBinaryDataField {

	/**
	 * Check whether the binary data represents an image.
	 * 
	 * @return
	 */
	default boolean hasProcessableImage() {
		return NodeUtil.isProcessableImage(getMimeType());
	}

	/**
	 * Set the binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	HibImageDataField setImageDominantColor(String dominantColor);

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
	HibImageDataField setImageFocalPoint(FocalPoint point);

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
	default BinaryMetadata getMetadata() {
		BinaryMetadata metaData = new BinaryMetadata();
		for (Map.Entry<String, String> entry : getMetadataProperties().entrySet()) {
			metaData.add(entry.getKey(), entry.getValue());
		}

		// Now set the GPS information
		Double lat = getLocationLatitude();
		Double lon = getLocationLongitude();
		if (lat != null && lon != null) {
			metaData.setLocation(lon, lat);
		}
		Integer alt = getLocationAltitude();
		if (alt != null && metaData.getLocation() != null) {
			metaData.getLocation().setAlt(alt);
		}
		return metaData;
	}

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
	
	@Override
	HibImageDataElement getBinary();
}

package com.gentics.mesh.core.data.node.field;

import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NodeUtil;
import com.gentics.mesh.util.UniquenessUtil;

public interface HibBinaryField extends HibField, HibBasicField<BinaryField>, HibDeletableField, HibDisplayField {

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
	default boolean hasProcessableImage() {
		return NodeUtil.isProcessableImage(getMimeType());
	}

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
	default BinaryField transformToRest(ActionContext ac) {
		BinaryField restModel = new BinaryFieldImpl();
		restModel.setFileName(getFileName());
		restModel.setMimeType(getMimeType());

		HibBinary binary = getBinary();
		if (binary != null) {
			restModel.setBinaryUuid(binary.getUuid());
			restModel.setFileSize(binary.getSize());
			restModel.setSha512sum(binary.getSHA512Sum());
			restModel.setWidth(binary.getImageWidth());
			restModel.setHeight(binary.getImageHeight());
		}

		restModel.setFocalPoint(getImageFocalPoint());
		restModel.setDominantColor(getImageDominantColor());

		BinaryMetadata metaData = getMetadata();
		restModel.setMetadata(metaData);

		restModel.setPlainText(getPlainText());
		return restModel;
	}

	@Override
	default void validate() {

	}

	/**
	 * Common comparison code.
	 * 
	 * @param obj
	 * @return
	 */
	default boolean binaryFieldEquals(Object obj) {
		if (getClass().isInstance(obj)) {
			HibBinaryField binaryField = getClass().cast(obj);
			String filenameA = getFileName();
			String filenameB = binaryField.getFileName();
			boolean filename = Objects.equals(filenameA, filenameB);

			String mimeTypeA = getMimeType();
			String mimeTypeB = binaryField.getMimeType();
			boolean mimetype = Objects.equals(mimeTypeA, mimeTypeB);

			HibBinary binaryA = getBinary();
			HibBinary binaryB = binaryField.getBinary();

			String hashSumA = binaryA != null ? binaryA.getSHA512Sum() : null;
			String hashSumB = binaryB != null ? binaryB.getSHA512Sum() : null;
			boolean sha512sum = Objects.equals(hashSumA, hashSumB);
			return filename && mimetype && sha512sum;
		}
		if (obj instanceof BinaryField) {
			BinaryField binaryField = (BinaryField) obj;

			boolean matchingFilename = true;
			if (binaryField.getFileName() != null) {
				String filenameA = getFileName();
				String filenameB = binaryField.getFileName();
				matchingFilename = Objects.equals(filenameA, filenameB);
			}

			boolean matchingMimetype = true;
			if (binaryField.getMimeType() != null) {
				String mimeTypeA = getMimeType();
				String mimeTypeB = binaryField.getMimeType();
				matchingMimetype = Objects.equals(mimeTypeA, mimeTypeB);
			}

			boolean matchingFocalPoint = true;
			if (binaryField.getFocalPoint() != null) {
				FocalPoint pointA = getImageFocalPoint();
				FocalPoint pointB = binaryField.getFocalPoint();
				matchingFocalPoint = Objects.equals(pointA, pointB);
			}

			boolean matchingDominantColor = true;
			if (binaryField.getDominantColor() != null) {
				String colorA = getImageDominantColor();
				String colorB = binaryField.getDominantColor();
				matchingDominantColor = Objects.equals(colorA, colorB);
			}

			boolean matchingSha512sum = true;
			if (binaryField.getSha512sum() != null) {
				String hashSumA = getBinary() != null ? getBinary().getSHA512Sum() : null;
				String hashSumB = binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}

			boolean matchingMetadata = true;
			if (binaryField.getMetadata() != null) {
				BinaryMetadata graphMetadata = getMetadata();
				BinaryMetadata restMetadata = binaryField.getMetadata();
				matchingMetadata = Objects.equals(graphMetadata, restMetadata);
			}
			return matchingFilename && matchingMimetype && matchingFocalPoint && matchingDominantColor && matchingSha512sum && matchingMetadata;
		}
		return false;
	}

	@Override
	default String getDisplayName() {
		return getFileName();
	}
}

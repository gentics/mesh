package com.gentics.mesh.core.data.s3binary;


import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.data.node.field.HibBasicField;
import com.gentics.mesh.core.data.node.field.HibDisplayField;
import com.gentics.mesh.core.data.node.field.HibImageDataField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.impl.S3BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;
import com.gentics.mesh.handler.ActionContext;

/**
 * MDM interface for the s3binary field information.
 */
public interface S3HibBinaryField extends HibImageDataField, HibBasicField<S3BinaryField>, HibDeletableField, HibDisplayField {

	/**
	 * Set the S3 Object key
	 * @param key
	 */
	void setS3ObjectKey(String key);
	   
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
	 * Set the s3 file size
	 * @param size
	 */
    void setFileSize(Long size);

	/**
	 * Copy the binary data and metadata to another S3 binary field.
	 */
	S3HibBinaryField copyTo(S3HibBinaryField field);

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	default S3BinaryMetadata getMetadata() {
		S3BinaryMetadata metaData = new S3BinaryMetadata();
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

	@Override
	default S3BinaryField transformToRest(ActionContext ac) {
		S3BinaryField restModel = new S3BinaryFieldImpl();

		S3HibBinary binary = getBinary();
		if (binary != null) {
			restModel.setS3binaryUuid(binary.getUuid());
			restModel.setS3ObjectKey(binary.getS3ObjectKey());
			restModel.setFileName(binary.getFileName());
			restModel.setFileSize(binary.getSize());
			restModel.setWidth(binary.getImageWidth());
			restModel.setHeight(binary.getImageHeight());
		}
		S3BinaryMetadata metaData = getMetadata();
		restModel.setMetadata(metaData);
		restModel.setMimeType(getMimeType());
		restModel.setFileSize(getFileSize());
		restModel.setDominantColor(getImageDominantColor());
		return restModel;
	}

	@Override
	default void validate() {

	}

	@Override
	default String getDisplayName() {
		return getFileName();
	}

	@Override
	S3HibBinary getBinary();

	default boolean s3BinaryFieldEquals(Object obj) {
		if (getClass().isInstance(obj)) {
			S3HibBinaryField s3binaryField = getClass().cast(obj);
			String filenameA = getFileName();
			String filenameB = s3binaryField.getFileName();
			boolean filename = Objects.equals(filenameA, filenameB);

			String mimeTypeA = getMimeType();
			String mimeTypeB = s3binaryField.getMimeType();
			boolean mimetype = Objects.equals(mimeTypeA, mimeTypeB);

			S3HibBinary s3binaryA = getBinary();
			S3HibBinary s3binaryB = s3binaryField.getBinary();

			String s3ObjectKeyA = s3binaryA != null ? s3binaryA.getS3ObjectKey() : null;
			String s3ObjectKeyB = s3binaryB != null ? s3binaryB.getS3ObjectKey() : null;
			boolean s3ObjectKey = Objects.equals(s3ObjectKeyA, s3ObjectKeyB);
			return filename && mimetype && s3ObjectKey;
		}
		if (obj instanceof S3BinaryField) {
			S3BinaryField s3binaryField = (S3BinaryField) obj;

			boolean matchingS3ObjectKey = true;
			if (s3binaryField.getS3ObjectKey() != null) {
				String s3ObjectKeyA = getS3ObjectKey();
				String s3ObjectKeyB = s3binaryField.getS3ObjectKey();
				matchingS3ObjectKey = Objects.equals(s3ObjectKeyA, s3ObjectKeyB);
			}

			boolean matchingFilename = true;
			if (s3binaryField.getFileName() != null) {
				String filenameA = getFileName();
				String filenameB = s3binaryField.getFileName();
				matchingFilename = Objects.equals(filenameA, filenameB);
			}

			boolean matchingMimetype = true;
			if (s3binaryField.getMimeType() != null) {
				String mimeTypeA = getMimeType();
				String mimeTypeB = s3binaryField.getMimeType();
				matchingMimetype = Objects.equals(mimeTypeA, mimeTypeB);
			}

			boolean matchingFocalPoint = true;
			if (s3binaryField.getFocalPoint() != null) {
				FocalPoint pointA = getImageFocalPoint();
				FocalPoint pointB = s3binaryField.getFocalPoint();
				matchingFocalPoint = Objects.equals(pointA, pointB);
			}

			boolean matchingDominantColor = true;
			if (s3binaryField.getDominantColor() != null) {
				String colorA = getImageDominantColor();
				String colorB = s3binaryField.getDominantColor();
				matchingDominantColor = Objects.equals(colorA, colorB);
			}

			boolean matchingSha512sum = true;
			if (s3binaryField.getS3ObjectKey() != null) {
				String hashSumA = getBinary() != null ? getBinary().getS3ObjectKey() : null;
				String hashSumB = s3binaryField.getS3ObjectKey();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}

			boolean matchingMetadata = true;
			if (s3binaryField.getMetadata() != null) {
				S3BinaryMetadata graphMetadata = getMetadata();
				S3BinaryMetadata restMetadata = s3binaryField.getMetadata();
				matchingMetadata = Objects.equals(graphMetadata, restMetadata);
			}
			return matchingFilename && matchingMimetype && matchingFocalPoint && matchingDominantColor && matchingSha512sum && matchingMetadata && matchingS3ObjectKey;
		}
		return false;
	}
}

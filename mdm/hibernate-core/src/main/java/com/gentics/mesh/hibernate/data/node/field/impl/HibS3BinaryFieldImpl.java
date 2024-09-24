package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.Map;
import java.util.UUID;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Amazon S3 Binary field edge wrapper implementation.
 * 
 * @author plyhun
 *
 */
public class HibS3BinaryFieldImpl extends AbstractDeletableHibField<HibS3BinaryFieldEdgeImpl> implements HibS3BinaryFieldBase {

	public HibS3BinaryFieldImpl(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, HibS3BinaryFieldEdgeImpl value) {
		super(value.getFieldKey(), parent, FieldTypes.S3BINARY, value);
	}

	public HibS3BinaryFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID edgeUuid) {
		super(fieldKey, parent, FieldTypes.S3BINARY, edgeUuid, HibS3BinaryFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		S3HibBinaryField binary = container.createS3Binary(getFieldKey(), getBinary());
		return copyTo(binary);
	}

	@Override
	public S3HibBinaryField copyTo(S3HibBinaryField field) {
		return getReferencedEdge().copyTo(field);
	}

	@Override
	public S3HibBinary getBinary() {
		return getReferencedEdge().getBinary();
	}

	public void setS3Binary(S3HibBinary s3Binary) {
		getReferencedEdge().setBinary(s3Binary);
	}

	@Override
	public String getFileName() {
		return getReferencedEdge().getFileName();
	}

	@Override
	public S3HibBinaryField setFileName(String fileName) {
		getReferencedEdge().setFileName(fileName);
		return this;
	}

	@Override
	public String getMimeType() {
		return getReferencedEdge().getMimeType();
	}

	@Override
	public S3HibBinaryField setMimeType(String mimeType) {
		getReferencedEdge().setMimeType(mimeType);
		return this;
	}

	@Override
	public String getImageDominantColor() {
		return getReferencedEdge().getImageDominantColor();
	}

	@Override
	public S3HibBinaryField setImageDominantColor(String imageDominantColor) {
		getReferencedEdge().setImageDominantColor(imageDominantColor);
		return this;
	}

	@Override
	public FocalPoint getImageFocalPoint() {
		return getReferencedEdge().getImageFocalPoint();
	}

	@Override
	public S3HibBinaryField setImageFocalPoint(FocalPoint point) {
		return (S3HibBinaryField) getReferencedEdge().setImageFocalPoint(point);
	}

	@Override
	public void setMetadata(String key, String value) {
		getReferencedEdge().setMetadata(key, value);
	}

	@Override
	public Map<String, String> getMetadataProperties() {
		return getReferencedEdge().getMetadataProperties();
	}

	public void setMetadataProperties(Map<String, String> properties) {
		getReferencedEdge().setMetadataProperties(properties);
	}

	@Override
	public void clearMetadata() {
		getReferencedEdge().clearMetadata();
	}

	@Override
	public Double getLocationLatitude() {
		return getReferencedEdge().getLocationLatitude();
	}

	@Override
	public void setLocationLatitude(Double locationLatitude) {
		getReferencedEdge().setLocationLatitude(locationLatitude);
	}

	@Override
	public Double getLocationLongitude() {
		return getReferencedEdge().getLocationLongitude();
	}

	@Override
	public void setLocationLongitude(Double locationLongitude) {
		getReferencedEdge().setLocationLongitude(locationLongitude);
	}

	@Override
	public Integer getLocationAltitude() {
		return getReferencedEdge().getLocationAltitude();
	}

	@Override
	public void setLocationAltitude(Integer locationAltitude) {
		getReferencedEdge().setLocationAltitude(locationAltitude);
	}

	@Override
	public String getPlainText() {
		return getReferencedEdge().getPlainText();
	}

	@Override
	public void setPlainText(String plainText) {
		getReferencedEdge().setPlainText(plainText);
	}

	@Override
	public String getS3ObjectKey() {
		return getReferencedEdge().getS3ObjectKey();
	}

	@Override
	public void setS3ObjectKey(String s3ObjectKey) {
		getReferencedEdge().setS3ObjectKey(s3ObjectKey);
	}

	@Override
	public Long getFileSize() {
		return getReferencedEdge().getFileSize();
	}

	@Override
	public void setFileSize(Long fileSize) {
		getReferencedEdge().setFileSize(fileSize);
	}

	@Override
	public boolean equals(Object obj) {
		return s3BinaryFieldEquals(obj);
	}

	@Override
	public HibS3BinaryFieldEdgeImpl getReferencedEdge() {
		return HibernateTx.get().entityManager()
				.find(HibS3BinaryFieldEdgeImpl.class, value.get());
	}
}

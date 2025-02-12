package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.Map;
import java.util.UUID;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Binary field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibBinaryFieldImpl extends AbstractDeletableHibField<HibBinaryFieldEdgeImpl> implements HibBinaryFieldBase {

	public HibBinaryFieldImpl(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, HibBinaryFieldEdgeImpl value) {
		super(value.getFieldKey(), parent, FieldTypes.BINARY, value);
	}

	public HibBinaryFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, UUID edgeUuid) {
		super(fieldKey, parent, FieldTypes.BINARY, edgeUuid, HibBinaryFieldEdgeImpl.class);
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibBinaryField binary = container.createBinary(getFieldKey(), getBinary());
		return copyTo(binary);
	}

	@Override
	public String getFileName() {
		return getReferencedEdge().getFileName();
	}

	@Override
	public HibBinaryField setFileName(String fileName) {
		getReferencedEdge().setFileName(fileName);
		return this;
	}

	@Override
	public HibBinaryField copyTo(HibBinaryField targetField) {
		return getReferencedEdge().copyTo(targetField);
	}

	@Override
	public String getMimeType() {
		return getReferencedEdge().getMimeType();
	}

	@Override
	public HibBinaryField setMimeType(String mimeType) {
		getReferencedEdge().setMimeType(mimeType);
		return this;
	}

	@Override
	public String getImageDominantColor() {
		return getReferencedEdge().getImageDominantColor();
	}

	@Override
	public HibBinaryField setImageDominantColor(String dominantColor) {
		getReferencedEdge().setImageDominantColor(dominantColor);
		return this;
	}

	@Override
	public FocalPoint getImageFocalPoint() {
		return getReferencedEdge().getImageFocalPoint();
	}

	@Override
	public HibBinaryField setImageFocalPoint(FocalPoint point) {
		return (HibBinaryField) getReferencedEdge().setImageFocalPoint(point);
	}

	@Override
	public HibBinary getBinary() {
		return getReferencedEdge().getBinary();
	}

	public void setBinary(HibBinary binary) {
		getReferencedEdge().setBinary(binary);
	}

	@Override
	public void setMetadata(String key, String value) {
		getReferencedEdge().setMetadata(key, value);		
	}

	@Override
	public Map<String, String> getMetadataProperties() {
		return getReferencedEdge().getMetadataProperties();
	}

	public void setMetadataProperties(Map<String, String> metadataProperties) {
		getReferencedEdge().setMetadataProperties(metadataProperties);
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
	public void setLocationLatitude(Double lat) {
		getReferencedEdge().setLocationLatitude(lat);
	}

	@Override
	public Double getLocationLongitude() {
		return getReferencedEdge().getLocationLongitude();
	}

	@Override
	public void setLocationLongitude(Double lon) {
		getReferencedEdge().setLocationLongitude(lon);
	}

	@Override
	public Integer getLocationAltitude() {
		return getReferencedEdge().getLocationAltitude();
	}

	@Override
	public void setLocationAltitude(Integer alt) {
		getReferencedEdge().setLocationAltitude(alt);
	}

	@Override
	public void setPlainText(String text) {
		getReferencedEdge().setPlainText(text);
	}

	@Override
	public String getPlainText() {
		return getReferencedEdge().getPlainText();
	}

	@Override
	public boolean equals(Object obj) {
		return binaryFieldEquals(obj);
	}

	@Override
	public HibBinaryFieldEdgeImpl getReferencedEdge() {
		return HibernateTx.get().entityManager()
				.find(HibBinaryFieldEdgeImpl.class, value.get());
	}

	@Override
	public Result<? extends HibImageVariant> getImageVariants() {
		return new TraversalResult<>(HibernateTx.get().entityManager().createNamedQuery("imagevariant_find_all_by_field", HibImageVariantImpl.class).setParameter("field", getReferencedEdge()).getResultList());
	}

	@Override
	public HibNodeFieldContainer getParentContainer() {
		return (HibNodeFieldContainer) getContainer();
	}

	@Override
	public HibBinaryFieldEdgeImpl getEdge() {
		return getReferencedEdge();
	}
}
package com.gentics.mesh.hibernate.data.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.node.field.HibImageDataField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.MeshOptions;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;

/**
 * Common part of a binary field edge entity.
 * 
 * @author plyhun
 *
 * @param <B>
 */
@MappedSuperclass
public abstract class AbstractBinaryFieldEdgeImpl<B extends HibImageDataElement> extends AbstractFieldEdgeImpl<UUID> implements HibImageDataField  {

	public static final int METADATA_PROPERTY_LENGTH = MeshOptions.DEFAULT_STRING_LENGTH;

	protected String fileName;
	protected String mimeType;
	protected String imageDominantColor;
	protected Float focalPointX;
	protected Float focalPointY;
	protected Double locationLatitude;
	protected Double locationLongitude;
	protected Integer locationAltitude;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@ElementCollection
	@CollectionTable
	@MapKeyColumn(name = "pkey")
	@Column(name = "pvalue", length = METADATA_PROPERTY_LENGTH)
	private Map<String, String> metadataProperties = new HashMap<>();

	@Column(length = MeshOptions.DEFAULT_STRING_LENGTH)
	protected String plainText;

	public AbstractBinaryFieldEdgeImpl() {
		super();
	}
	public AbstractBinaryFieldEdgeImpl(HibernateTx tx, String fieldKey, B binary, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, (UUID) binary.getId(), parentFieldContainer);
	}

	protected abstract Class<? extends B> getImageEntityClass();

	@Override
	public B getBinary() {
		return HibernateTx.get().load(valueOrUuid, getImageEntityClass());
	}

	public void setBinary(B binary) {
		valueOrUuid = ((UUID) binary.getId());
	}

	@Override
	public void setPlainText(String text) {
		plainText = text;
	}

	@Override
	public String getPlainText() {
		return plainText;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public AbstractBinaryFieldEdgeImpl<B> setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public AbstractBinaryFieldEdgeImpl<B> setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	@Override
	public Double getLocationLatitude() {
		return locationLatitude;
	}

	@Override
	public void setLocationLatitude(Double lat) {
		locationLatitude = lat;
	}

	@Override
	public Double getLocationLongitude() {
		return locationLongitude;
	}

	@Override
	public void setLocationLongitude(Double lon) {
		locationLongitude = lon;
	}

	@Override
	public Integer getLocationAltitude() {
		return locationAltitude;
	}

	@Override
	public void setLocationAltitude(Integer alt) {
		locationAltitude = alt;
	}

	@Override
	public String getImageDominantColor() {
		return imageDominantColor;
	}

	@Override
	public AbstractBinaryFieldEdgeImpl<B> setImageDominantColor(String dominantColor) {
		imageDominantColor = dominantColor;
		return this;
	}

	@Override
	public FocalPoint getImageFocalPoint() {
		if (focalPointX == null || focalPointY == null) {
			return null;
		}
		return new FocalPoint(focalPointX, focalPointY);
	}

	@Override
	public AbstractBinaryFieldEdgeImpl<B> setImageFocalPoint(FocalPoint point) {
		if (point != null) {
			focalPointX = point.getX();
			focalPointY = point.getY();
		}
		return this;
	}

	@Override
	public void setMetadata(String key, String value) {
		metadataProperties.put(key, value);
	}

	@Override
	public Map<String, String> getMetadataProperties() {
		return metadataProperties;
	}

	public void setMetadataProperties(Map<String, String> metadataProperties) {
		this.metadataProperties.putAll(metadataProperties);
	}

	@Override
	public void clearMetadata() {
		setLocationAltitude(null);
		setLocationLongitude(null);
		setLocationLatitude(null);
		metadataProperties.clear();
	}
}

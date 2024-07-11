package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractDeletableHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.HibS3BinaryFieldBase;
import com.gentics.mesh.hibernate.data.node.field.impl.HibS3BinaryFieldImpl;

/**
 * Amazon S3 Binary field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "s3binaryfieldref")
@NamedQueries({
	@NamedQuery(
			name = "s3binaryfieldref.getByContentAndFieldKey",
			query = "select s from s3binaryfieldref s where s.containerUuid = :contentUuid and s.fieldKey = :fieldKey"),
	@NamedQuery(
			name = "s3binaryfieldref.findByContent",
			query = "select s from s3binaryfieldref s where s.containerUuid = :contentUuid"),
	@NamedQuery(
			name = "s3binaryfieldref.findByUuid",
			query = "select s from s3binaryfieldref s where s.dbUuid = :uuid"),
	@NamedQuery(
			name = "s3binaryfieldref.removeByContainerUuids",
			query = "delete from s3binaryfieldref where containerUuid in :containerUuids"),
})
@Table(
		indexes = {
				@Index(name = "idx_content_field_key", columnList = "containerUuid, fieldKey")
		},
		uniqueConstraints = { 
				@UniqueConstraint(
						name = "KeyTypeVersionContainer", 
						columnNames = { "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
				) 
		}
)
public class HibS3BinaryFieldEdgeImpl extends AbstractBinaryFieldEdgeImpl<S3Binary> implements HibS3BinaryFieldBase, Serializable {

	private static final long serialVersionUID = 8083028337267324709L;

	private String s3ObjectKey;
	private Long fileSize;

	public HibS3BinaryFieldEdgeImpl() {
	}

	protected HibS3BinaryFieldEdgeImpl(HibernateTx tx, String fieldKey, S3Binary binary, HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, binary, parentFieldContainer);
	}

	@Override
	public Field cloneTo(FieldContainer dst) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> dstBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) dst;
		dstBase.ensureColumnExists(getFieldKey(), FieldTypes.S3BINARY);
		dstBase.ensureOldReferenceRemoved(tx, getFieldKey(), dstBase::getS3Binary, false);
		return new HibS3BinaryFieldImpl(
					dstBase, HibS3BinaryFieldEdgeImpl.fromContainer(tx, dstBase, getFieldKey(), (HibS3BinaryImpl) getBinary()));
	}

	@Override
	public S3BinaryField copyTo(S3BinaryField field) {
		HibS3BinaryFieldBase target = HibS3BinaryFieldBase.class.cast(field);
		if (AbstractDeletableHibField.class.isInstance(target)) {
			target = HibS3BinaryFieldBase.class.cast(((AbstractDeletableHibField.class.cast(target)).getReferencedEdge()));
		}
		target.setFieldKey(getFieldKey());
		target.setFileName(getFileName());
		target.setMimeType(getMimeType());
		target.setImageDominantColor(getImageDominantColor());
		target.setImageFocalPoint(getImageFocalPoint());
		target.setMetadataProperties(getMetadataProperties());
		target.setLocationLatitude(getLocationLatitude());
		target.setLocationLongitude(getLocationLongitude());
		target.setLocationAltitude(getLocationAltitude());
		target.setPlainText(getPlainText());
		target.setS3ObjectKey(getS3ObjectKey());
		target.setFileSize(getFileSize());

		return target;
	}

	@Override
	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	@Override
	public void setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
	}

	@Override
	public Long getFileSize() {
		return fileSize;
	}

	@Override
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	public boolean equals(Object obj) {
		return s3BinaryFieldEquals(obj);
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		tx.s3binaryDao().removeField(bac, this);
	}

	public static HibS3BinaryFieldEdgeImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?,?,?,?,?> container, String fieldKey, HibS3BinaryImpl binary) {
		HibS3BinaryFieldEdgeImpl edge = new HibS3BinaryFieldEdgeImpl(tx, fieldKey, binary, container);
		tx.entityManager().persist(edge);
		return edge;
	}

	@Override
	protected Class<? extends S3Binary> getImageEntityClass() {
		return HibS3BinaryImpl.class;
	}

	@Override
	public String getFileName() {
		// TODO FIXME two filename fields make mess.
		String filename = super.getFileName();
		return StringUtils.isBlank(filename) ? getBinary().getFileName() : filename;
	}
}


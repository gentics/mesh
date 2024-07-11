package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractDeletableHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBinaryFieldBase;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBinaryFieldImpl;

/**
 * Binary field definition edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "binaryfieldref")
@NamedEntityGraph(name = "binaryfieldref.metadataProperties", attributeNodes = @NamedAttributeNode("metadataProperties"))
@NamedQueries({
		@NamedQuery(
			name = "binaryfieldref.findByBinaryUuid",
			query = "select bf from binaryfieldref bf where bf.valueOrUuid = :dbUuid"),
		@NamedQuery(
			name = "binaryfieldref.findByContentAndKey",
			query = "select f from binaryfieldref  f where f.containerUuid = :contentUuid and f.fieldKey = :key"),
		@NamedQuery(
			name = "binaryfieldref.findByContent",
			query = "select b from binaryfieldref b where b.containerUuid = :contentUuid"),
		@NamedQuery(
			name = "binaryfieldref.findByUuid",
			query = "select b from binaryfieldref b where b.dbUuid = :uuid"),
		@NamedQuery(
			name = "binaryfieldref.removeByContainerUuids",
			query = "delete from binaryfieldref where containerUuid in :containerUuids"),
		@NamedQuery(
			name = "binaryfieldref.findByUuids",
			query = "select b from binaryfieldref b where b.dbUuid in :uuids")
})
@Table(
		indexes = {
				@Index(name = "idx_binary", columnList = "valueOrUuid"),
				@Index(name = "idx_content_key", columnList = "containerUuid, fieldKey"),
		},
		uniqueConstraints = { 
				@UniqueConstraint(
						name = "KeyTypeVersionContainer", 
						columnNames = { "fieldKey", "containerUuid", "containerType", "containerVersionUuid" }
				) 
		}
)
public class HibBinaryFieldEdgeImpl extends AbstractBinaryFieldEdgeImpl<Binary> implements HibBinaryFieldBase, Serializable {

	private static final long serialVersionUID = -192130888476185291L;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "binary_field_variant", inverseJoinColumns = {@JoinColumn(name = "variants_dbUuid")}, joinColumns = {@JoinColumn(name = "fields_dbUuid")})
	protected Set<HibImageVariantImpl> variants = new HashSet<>();

	public HibBinaryFieldEdgeImpl() {
	}

	protected HibBinaryFieldEdgeImpl(HibernateTx tx, String fieldKey, Binary binary, HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super(tx, fieldKey, binary, parentFieldContainer);
	}

	@Override
	public Field cloneTo(FieldContainer dst) {
		HibernateTx tx = HibernateTx.get();
		HibUnmanagedFieldContainer<?, ?, ?, ?, ?> dstBase = (HibUnmanagedFieldContainer<?,?,?,?,?>) dst;
		dstBase.ensureColumnExists(getFieldKey(), FieldTypes.BINARY);
		dstBase.ensureOldReferenceRemoved(tx, getFieldKey(), dstBase::getBinary, false);
		return new HibBinaryFieldImpl(
				dstBase, HibBinaryFieldEdgeImpl.fromContainer(tx, dstBase, getFieldKey(), (HibBinaryImpl) getBinary()));
	}

	@Override
	public BinaryField copyTo(BinaryField targetField) {
		HibBinaryFieldBase target = HibBinaryFieldBase.class.cast(targetField);
		if (AbstractDeletableHibField.class.isInstance(target)) {
			target = HibBinaryFieldBase.class.cast(((AbstractDeletableHibField.class.cast(target)).getReferencedEdge()));
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

		for (ImageVariant variant : getImageVariants()) {
			HibernateTx.get().imageVariantDao().attachVariant(target, (HibImageVariantImpl) variant, false);
		}

		return targetField;
	}

	@Override
	public boolean equals(Object obj) {
		return binaryFieldEquals(obj);
	}

	@Override
	public void onEdgeDeleted(HibernateTx tx, BulkActionContext bac) {
		tx.binaryDao().removeField(bac, this);
	}

	public static HibBinaryFieldEdgeImpl fromContainer(HibernateTx tx, HibUnmanagedFieldContainer<?,?,?,?,?> container, String fieldKey, HibBinaryImpl binary) {
		HibBinaryFieldEdgeImpl edge = new HibBinaryFieldEdgeImpl(tx, fieldKey, binary, container);
		tx.entityManager().persist(edge);
		return edge;
	}

	@Override
	protected Class<? extends Binary> getImageEntityClass() {
		return HibBinaryImpl.class;
	}

	@Override
	public NodeFieldContainer getParentContainer() {
		HibernateTx tx = HibernateTx.get();
		return tx.contentDao().getFieldContainer(tx.load(getContainerVersionUuid(), HibSchemaVersionImpl.class), getContainerUuid());
	}

	@Override
	public Result<? extends ImageVariant> getImageVariants() {
		return new TraversalResult<>(variants);
	}

	@Override
	public HibBinaryFieldEdgeImpl getEdge() {
		return this;
	}
}


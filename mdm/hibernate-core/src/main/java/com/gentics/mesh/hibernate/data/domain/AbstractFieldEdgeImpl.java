package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.database.HibernateTx;

/**
 * The field base for the field types, stored as either a reference from the container, or a list item.
 * 
 * @author plyhun
 *
 * @param <VU> stored value type, either primitive or UUID
 */
@MappedSuperclass
public abstract class AbstractFieldEdgeImpl<VU> implements HibFieldEdge, Field {

	@Id
	private UUID dbUuid;
	@Column(nullable = false)
	private String fieldKey;
	@Column(nullable = false)
	private UUID containerUuid;
	@Column(nullable = false)
	private UUID containerVersionUuid;
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ReferenceType containerType;
	
	@Column(nullable = true)
	protected VU valueOrUuid;

	/**
	 * Empty constructor for the Hibernate data fetch mechanism.
	 */
	public AbstractFieldEdgeImpl() {
	}

	/**
	 * Initial constructor.
	 * 
	 * @param tx initializing transaction
	 * @param fieldKey parent key field
	 * @param valueOrUuid stored value, either a primivive or {@link UUID}
	 * @param parentFieldContainer parent container
	 */
	public AbstractFieldEdgeImpl(HibernateTx tx, String fieldKey, VU valueOrUuid, 
			HibUnmanagedFieldContainer<?,?,?,?,?> parentFieldContainer) {
		super();
		this.dbUuid = tx.uuidGenerator().generateType1UUID();
		this.fieldKey = fieldKey;
		this.valueOrUuid = valueOrUuid;
		this.containerUuid = parentFieldContainer.getDbUuid();
		this.containerVersionUuid = (UUID) parentFieldContainer.getSchemaContainerVersion().getId();
		this.containerType = parentFieldContainer.getReferenceType();
	}

	@Override
	public void validate() {
		if (containerUuid == null || containerVersionUuid == null) {
			throw error(BAD_REQUEST, "Field Container info missing: UUID " + containerUuid + " / Version " + containerVersionUuid);
		}
		if (valueOrUuid == null) {
			throw error(BAD_REQUEST, "No value provided");
		}
	}

	/**
	 * Get the ending node container that references this field.
	 * 
	 * @return
	 */
	public Stream<HibNodeFieldContainerImpl> getReferencingContents(boolean lookupInContent, boolean lookupInMicronode) {
		HibernateTx tx = HibernateTx.get();
		switch (containerType) {
		case FIELD:
			return lookupInContent ? Stream.ofNullable(
						tx.contentDao().getFieldContainer(tx.load(containerVersionUuid, HibSchemaVersionImpl.class), containerUuid)) : Stream.empty();
		case MICRONODE:
			return lookupInMicronode ? Stream.ofNullable(
						tx.contentDao().getFieldContainer(tx.load(containerVersionUuid, HibMicroschemaVersionImpl.class), containerUuid)
					).flatMap(Micronode::getContents).map(HibNodeFieldContainerImpl.class::cast) : Stream.empty();
		default:
			break;
		}
		throw new IllegalStateException("Unsupported HibNodeFieldEdge.outType: " + containerType);
	}

	/**
	 * Get the container that references this field (node/micronode).
	 * 
	 * @return
	 */
	public Stream<? extends HibUnmanagedFieldContainer<?,?,?,?,?>> getReferencingContainers() {
		HibernateTx tx = HibernateTx.get();
		switch (containerType) {
		case FIELD:
			return Stream.ofNullable(
						tx.contentDao().getFieldContainer(tx.load(containerVersionUuid, HibSchemaVersionImpl.class), containerUuid));
		case MICRONODE:
			return Stream.ofNullable(
						tx.contentDao().getFieldContainer(tx.load(containerVersionUuid, HibMicroschemaVersionImpl.class), containerUuid));
		default:
			break;
		}
		throw new IllegalStateException("Unsupported HibNodeFieldEdge.outType: " + containerType);
	}

	/**
	 * Get the key, actually stored at this field, on contrary to {@link #getFieldKey()} API method, 
	 * which may return the key of a parent container field.
	 * 
	 * @return
	 */
	public String getStoredFieldKey() {
		return this.fieldKey;
	}

	@Override
	public UUID getDbUuid() {
		return dbUuid;
	}
	@Override
	public void setDbUuid(UUID uuid) {
		this.dbUuid = uuid;
	}

	@Override
	public Long getDbVersion() {
		return 0L;
	}

	@Override
	public UUID getContainerUuid() {
		return containerUuid;
	}
	/**
	 * Set the container UUID, the field belongs to.
	 */
	public void setContainerUuid(UUID outUuid) {
		this.containerUuid = outUuid;
	}

	@Override
	public UUID getContainerVersionUuid() {
		return containerVersionUuid;
	}

	@Override
	public ReferenceType getContainerType() {
		return containerType;
	}

	@Override
	public String getFieldKey() {
		return fieldKey;
	}

	@Override
	public void setFieldKey(String fieldKey) {
		this.fieldKey = fieldKey;
	}

	public VU getValueOrUuid() {
		return valueOrUuid;
	}

	@Override
	public String toString() {
		return "AbstractFieldEdgeImpl [dbUuid=" + dbUuid + ", fieldKey=" + fieldKey + ", containerUuid=" + containerUuid
				+ ", containerVersionUuid=" + containerVersionUuid + ", containerType=" + containerType
				+ ", valueOrUuid=" + valueOrUuid + "]";
	}
}

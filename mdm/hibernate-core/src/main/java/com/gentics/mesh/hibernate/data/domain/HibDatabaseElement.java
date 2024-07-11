package com.gentics.mesh.hibernate.data.domain;

import java.util.UUID;

import com.gentics.mesh.core.data.Element;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Interface for all uuid based elements
 */
public interface HibDatabaseElement extends Element {
	/**
	 * Gets the internal uuid.
	 * @return
	 */
	UUID getDbUuid();

	/**
	 * Sets the internal uuid.
	 * @param uuid
	 */
	void setDbUuid(UUID uuid);
	
	/**
	 * Gets the internal DB entity version
	 * 
	 * @return
	 */
	Long getDbVersion();

	/**
	 * Get the database entity name for the current record instance. This is the only way to get a name for unmanaged entities, as they depend on version UUID value.
	 * 
	 * @return
	 */
	default String getDatabaseEntityName() {
		return HibernateTx.get().data().getDatabaseConnector().maybeGetDatabaseEntityName(getClass())
				.orElseThrow(() -> new IllegalStateException("Class '" + getClass().getCanonicalName() + "' does not appear to have either a corresponding @Entity annotation, or an unmanaged implementation on an entity name."));
	}

	/**
	 * Get the database table name for the current record instance. This is the only way to get a name for unmanaged entities, as they depend on version UUID value.
	 * 
	 * @return
	 */
	default String getDatabaseTableName() {
		return MeshTablePrefixStrategy.TABLE_NAME_PREFIX + getDatabaseEntityName();
	}

	@Override
	default String getElementVersion() {
		return ETag.hash(getUuid() + getDbVersion());
	}

	@Override
	default UUID getId() {
		return getDbUuid();
	}

	@Override
	default String getUuid() {
		return UUIDUtil.toShortUuid(getDbUuid());
	}

	@Override
	default void setUuid(String uuid) {
		setDbUuid(UUIDUtil.toJavaUuid(uuid));
	}
}

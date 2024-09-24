package com.gentics.mesh.hibernate.data.domain;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Common part for a Hibernate-backed DB entity.
 * 
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractHibDatabaseElement implements HibDatabaseElement {

	@Id
	private UUID dbUuid;

	@Version
	private Long dbVersion;

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
		return dbVersion;
	}

	public void setDbVersion(Long version) {
		this.dbVersion = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractHibDatabaseElement)) return false;
		AbstractHibDatabaseElement that = (AbstractHibDatabaseElement) o;
		return Objects.equals(getDbUuid(), that.getDbUuid());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDbUuid());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [dbUuid=" + dbUuid + ", dbVersion=" + dbVersion + "]";
	}
}

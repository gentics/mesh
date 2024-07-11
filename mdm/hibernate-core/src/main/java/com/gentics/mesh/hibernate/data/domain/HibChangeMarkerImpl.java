package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import com.gentics.mesh.core.data.changelog.ChangeMarker;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * High level change marker entity implementation.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "changemarker")
public class HibChangeMarkerImpl implements ChangeMarker, HibDatabaseElement, Serializable {
	
	private static final long serialVersionUID = 1218407021523657677L;

	@Id
	private UUID dbUuid;
	@Version
	private long dbVersion;
	@Column
	private long duration;

	@Override
	public Long getDuration() {
		return duration;
	}

	@Override
	public void setDuration(long duration) {
		this.duration = duration;
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
		return dbVersion;
	}

	public void setDbVersion(Long version) {
		this.dbVersion = version;
	}

	@Override
	public String toString() {
		return "HibChangeMarkerImpl [dbUuid=" + dbUuid + ", dbVersion=" + dbVersion + ", duration=" + duration + "]";
	}	
}

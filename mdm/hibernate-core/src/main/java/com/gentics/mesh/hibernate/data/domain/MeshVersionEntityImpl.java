package com.gentics.mesh.hibernate.data.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Application version entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "version")
public class MeshVersionEntityImpl implements Serializable {
	private static final long serialVersionUID = -6656851988049381970L;

	/**
	 * There should only be one row in this table, which makes a primary key obsolete. However, hibernate
	 * always requires a primary key, so this is used as a dummy.
	 * TODO HIB Consider creating a consistency check that asserts the existence of exactly one row in this table.
	 */
	@Id
	private int dummyId;

	private String meshVersion;
	private String databaseRevision;

	public String getMeshVersion() {
		return meshVersion;
	}

	public MeshVersionEntityImpl setMeshVersion(String meshVersion) {
		this.meshVersion = meshVersion;
		return this;
	}

	public String getDatabaseRevision() {
		return databaseRevision;
	}

	public MeshVersionEntityImpl setDatabaseRevision(String databaseRevision) {
		this.databaseRevision = databaseRevision;
		return this;
	}
}

package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.core.data.MeshVersion;
import com.gentics.mesh.hibernate.data.domain.MeshVersionEntityImpl;

/**
 * Mesh version accessor implementation.
 * 
 * @author plyhun
 *
 */
public class MeshVersionImpl implements MeshVersion {
	private final EntityManager em;

	private MeshVersionEntityImpl instance;

	public MeshVersionImpl(EntityManager em) {
		this.em = em;
	}

	private MeshVersionEntityImpl getInstance() {
		if (instance == null) {
			instance = firstOrNull(em.createQuery("from version", MeshVersionEntityImpl.class));
			if (instance == null) {
				instance = new MeshVersionEntityImpl();
				em.persist(instance);
			}
		}

		return instance;
	}

	@Override
	public String getMeshVersion() {
		return getInstance().getMeshVersion();
	}

	@Override
	public void setMeshVersion(String version) {
		getInstance().setMeshVersion(version);
	}

	@Override
	public String getDatabaseRevision() {
		return getInstance().getDatabaseRevision();
	}

	@Override
	public void setDatabaseRevision(String databaseRevision) {
		getInstance().setDatabaseRevision(databaseRevision);
	}
}

package com.gentics.mesh.enterprise.database.connector;

import java.util.Collection;
import java.util.UUID;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.database.connector.AbstractDatabaseConnector;
import com.gentics.mesh.enterprise.server.license.LicenseService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

import jakarta.persistence.EntityManager;

/**
 * Base functionality for the Mesh Database Connector - licensed implementations.
 * 
 * @param <DR>
 * @param <DI>
 */
public abstract class AbstractEnterpriseDatabaseConnector extends AbstractDatabaseConnector {

	protected AbstractEnterpriseDatabaseConnector(HibernateMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("Usage of " + getClass().getSimpleName() + " requires a valid Gentics license!");
		}		
	}

	@Override
	public long deleteContentEdgesByProject(EntityManager em, HibProject project) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_PROJECT + ")")
				.setParameter("project", project)
				.executeUpdate();
	}

	@Override
	public long deleteContentEdgesByUuids(EntityManager em, Collection<UUID> uuids) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_NODE_UUIDS + ")")
				.setParameter("nodesUuid", uuids)
				.executeUpdate();
	}

	@Override
	public long deleteContentEdgesByBranchUuids(EntityManager em, HibBranch branch, Collection<UUID> uuids) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_NODE_UUIDS_BRANCH + ")")
				.setParameter("nodesUuid", uuids)
				.setParameter("branch", branch)
				.executeUpdate();
	}
}

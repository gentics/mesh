package com.gentics.mesh.enterprise.database.connector;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.database.connector.AbstractDatabaseConnector;
import com.gentics.mesh.enterprise.license.LicenseService;
import com.gentics.mesh.enterprise.license.LicensedFeature;
import com.gentics.mesh.enterprise.license.service.RequiresLicense;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

import jakarta.persistence.EntityManager;

/**
 * Base functionality for the Mesh Database Connector - licensed implementations.
 */
public abstract class AbstractEnterpriseDatabaseConnector extends AbstractDatabaseConnector implements RequiresLicense {

	private final LicenseService licenseService;

	protected AbstractEnterpriseDatabaseConnector(HibernateMeshOptions options, LicenseService licenseService) {
		super(options);
		this.licenseService = licenseService;
		if (licenseService == null) {
			licenseService = new LicenseService(options);
			if (!licenseService.isLicenseSufficient()) {
				throw new IllegalStateException("Usage of " + getClass().getSimpleName() + " requires a valid Gentics license!");
			}
		} else {
			licenseService.registerRequestor(this);
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

	@Override
	public Set<String> requiredFeatures() {
		return Collections.singleton(LicensedFeature.MSQL.getProductName());
	}

	public LicenseService getLicenseService() {
		return licenseService;
	}
}

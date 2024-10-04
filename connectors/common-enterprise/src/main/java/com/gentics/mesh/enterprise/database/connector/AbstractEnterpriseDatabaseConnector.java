package com.gentics.mesh.enterprise.database.connector;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.database.connector.AbstractDatabaseConnector;
import com.gentics.mesh.enterprise.service.LicenseCheckingAuthenticationService;
import com.gentics.mesh.enterprise.service.LicenseManagementEndpointService;
import com.gentics.mesh.enterprise.service.license.LicenseService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;

import jakarta.persistence.EntityManager;

/**
 * Base functionality for the Mesh Database Connector - licensed implementations.
 * 
 * @param <DR>
 * @param <DI>
 */
public abstract class AbstractEnterpriseDatabaseConnector extends AbstractDatabaseConnector {

	private static final Logger log = getLogger(AbstractEnterpriseDatabaseConnector.class);

	private final LicenseService licenseService;

	protected AbstractEnterpriseDatabaseConnector(HibernateMeshOptions options, RuntimeServiceRegistry runtimeServiceRegistry) {
		super(options);
		this.licenseService = new LicenseService(options);
		initAuthHandlers(runtimeServiceRegistry);
		initEndpointHandlers(runtimeServiceRegistry);
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

	private void initEndpointHandlers(RuntimeServiceRegistry runtimeServiceRegistry) {
		Optional<LicenseManagementEndpointService> maybeLicenseManagementEndpointService = runtimeServiceRegistry.endpointHandlers().stream()
				.filter(LicenseManagementEndpointService.class::isInstance)
				.map(LicenseManagementEndpointService.class::cast)
				.findAny();
		if (maybeLicenseManagementEndpointService.isPresent()) {
			maybeLicenseManagementEndpointService.get().init(this);
		} else {
			log.warn("This database connector provides a licensing management endpoint, which has not been loaded. Please check Mesh logs for the reasons.");
		}
	}

	private void initAuthHandlers(RuntimeServiceRegistry runtimeServiceRegistry) {
		Optional<LicenseCheckingAuthenticationService> maybeLicenseCheckingAuthService = runtimeServiceRegistry.authHandlers().stream()
				.filter(LicenseCheckingAuthenticationService.class::isInstance)
				.map(LicenseCheckingAuthenticationService.class::cast)
				.findAny();
		if (maybeLicenseCheckingAuthService.isPresent()) {
			maybeLicenseCheckingAuthService.get().init(this);
		} else if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("Usage of " + getClass().getSimpleName() + " requires a valid Gentics license!");
		}
	}

	public LicenseService getLicenseService() {
		return licenseService;
	}
}

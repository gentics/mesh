package com.gentics.mesh.database.connector;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.dialect.MariaDBBinaryUuidDialect;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;

import org.slf4j.Logger;
import jakarta.persistence.EntityManager;

/**
 * MariaDB 10.3+ Database connector.
 */
public class MariaDBConnector extends AbstractDatabaseConnector {

	private static final Logger log = getLogger(MariaDBConnector.class);

	public MariaDBConnector(HibernateMeshOptions options) {
		super(options);
	}

	@Override
	public String getConnectionUrl() {
		return "jdbc:mariadb://" 
				+ options.getStorageOptions().getDatabaseAddress() + "/"
				+ options.getStorageOptions().getDatabaseName() + "?"
				+ options.getStorageOptions().getConnectionUrlExtraParams();
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 999;
	}

	@Override
	public String getDummyComparison(Map<String, Object> params, boolean mustPass) {
		Boolean value = Boolean.valueOf(mustPass);
		String trueParam = HibernateUtil.makeParamName(value);
		params.put(trueParam, value);
		return " AND :" + trueParam + " ";
	}

	@Override
	public long deleteContentEdgesByProject(EntityManager em, Project project) {
		List<UUID> uuids = em.createQuery(FIND_BY_PROJECT, UUID.class)
			.setParameter("project", project)
			.getResultList();
		return SplittingUtils.splitAndCount(uuids, HibernateUtil.inQueriesLimitForSplitting(1), (size, slice) -> {
			int updated = em.createQuery(DELETE_BY_ELEMENT_IN + ":uuids")
				.setParameter("uuids", slice)
				.executeUpdate();
			log.info("{} of {} content edges deleted", updated, size);
			return (long) updated;
		});
	}

	@Override
	public long deleteContentEdgesByUuids(EntityManager em, Collection<UUID> uuids) {
		List<UUID> childUuids = em.createQuery(FIND_BY_NODE_UUIDS, UUID.class)
				.setParameter("nodesUuid", uuids)
				.getResultList();
		return SplittingUtils.splitAndCount(childUuids, HibernateUtil.inQueriesLimitForSplitting(1), (size, slice1) -> {
			int updated = em.createQuery(DELETE_BY_ELEMENT_IN + ":uuids")
				.setParameter("uuids", slice1)
				.executeUpdate();
			log.info("{} of {} content edges deleted", updated, size);
			return (long) updated;
		});
	}

	@Override
	public long deleteContentEdgesByBranchUuids(EntityManager em, Branch branch, Collection<UUID> uuids) {
		List<UUID> childUuids = em.createQuery(FIND_BY_NODE_UUIDS_BRANCH, UUID.class)
				.setParameter("nodesUuid", uuids)
				.setParameter("branch", branch)
				.getResultList();
		return SplittingUtils.splitAndCount(childUuids, HibernateUtil.inQueriesLimitForSplitting(1), (size, slice1) -> {
			int updated = em.createQuery(DELETE_BY_ELEMENT_IN + ":uuids")
				.setParameter("uuids", slice1)
				.executeUpdate();
			log.info("{} of {} content edges deleted", updated, size);
			return (long) updated;
		});
	}

	@Override
	protected String getDefaultDriverClassName() {
		return "org.mariadb.jdbc.Driver";
	}

	@Override
	protected String getDefaultDialectClassName() {
		return MariaDBBinaryUuidDialect.class.getCanonicalName();
	}
}

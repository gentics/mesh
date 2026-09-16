package com.gentics.mesh.database.connector;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.hibernate.dialect.MariaDBBinaryUuidDialect;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;

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
	public long deleteContentEdgesByProject(EntityManager em, HibProject project) {
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
	public long deleteContentEdgesByBranchUuids(EntityManager em, HibBranch branch, Collection<UUID> uuids) {
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

	@Override
	public int getStringLengthLimit() {
		// MariaDB uses "mediumtext"
		return 16_777_215;
	}

	@Override
	public Optional<Set<String>> getDatabaseColumnNames(Class<?> cls) {
		return super.getDatabaseColumnNames(cls)
			// TODO FIXME Somehow the JDBC driver at MariaDB does not update the cached metadata set,
			// so there is no legal way to retrieve the actual columns for the user.
			.map(set -> {
				if (cls.equals(HibUserImpl.class)) {
					return set.stream().filter(name -> !name.equalsIgnoreCase("apitokenid") && !name.equalsIgnoreCase("apitokenissuetimestamp")).collect(Collectors.toSet());
				} else {
					return set;
				}
			});
	}
}

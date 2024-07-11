package com.gentics.mesh.check;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.Query;
import org.hibernate.query.spi.Limit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfoModel;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.util.HibernateUtil;

import jakarta.persistence.EntityManager;

/**
 * Check consistency of entries in mesh_content_.... tables.
 * This will find records, which
 * <ol>
 * <li>belong to nodes, that do not exist any more</li>
 * <li>are not referenced in mesh_nodefieldcontainer or mesh_nodefieldcontainer_versions_edge</li>
 * </ol>
 */
public class ContentRefCheck extends AbstractHibernateConsistencyCheck {
	private static final Logger log = LoggerFactory.getLogger(ContentRefCheck.class);

	private HibernateDatabase db;

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		this.db = (HibernateDatabase) db;
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result, attemptRepair);
		return result;
	}

	/**
	 * Do the checks for all schemas and microschemas
	 * @param tx transaction
	 * @param result check result
	 * @param attemptRepair true to attempt repairing
	 */
	protected void checkInvalidRecordReferences(Tx tx, ConsistencyCheckResult result, boolean attemptRepair) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		DatabaseConnector dc = hibernateTx.data().getDatabaseConnector();
		SchemaDao schemaDao = tx.schemaDao();

		// check whether the table contains any entries, that reference inexistent contents

		// check for schemas
		Result<? extends Schema> schemas = schemaDao.findAll();
		for (Schema schema : schemas) {
			Iterable<? extends SchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (SchemaVersion version : versions) {
				String contentTable = dc.getPhysicalTableName(version);
				String nodeUuidColumn = dc.renderColumn(CommonContentColumn.NODE);
				String nodeTable = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "node";
				String uuidColumn = dc.renderNonContentColumn("dbuuid");
				String uuidColumnInContent = dc.renderColumn(CommonContentColumn.DB_UUID);
				String nodeFieldContainerTable = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "nodefieldcontainer";
				String contentUuidColumn = dc.renderNonContentColumn("contentuuid");
				String nodeFieldContainerVersionsEdgeTable = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "nodefieldcontainer_versions_edge";
				String thisContentUuidColumn = dc.renderNonContentColumn("thiscontentuuid");
				String nextContentUuidColumn = dc.renderNonContentColumn("nextcontentuuid");

				if (log.isDebugEnabled()) {
					log.debug("Check table {" + contentTable + "}");
				}

				checkCount(em, result, attemptRepair, contentTable, uuidColumnInContent, nodeUuidColumn, nodeTable, uuidColumn);
				checkCount(em, result, attemptRepair, contentTable, uuidColumnInContent, uuidColumnInContent,
						nodeFieldContainerTable, contentUuidColumn, nodeFieldContainerVersionsEdgeTable,
						thisContentUuidColumn, nodeFieldContainerVersionsEdgeTable, nextContentUuidColumn);
			}
		}
	}

	/**
	 * Check that in the source table, there are no records that reference records in the target tables, which do not exist in any of them
	 * @param em entity manager
	 * @param result check result, will get inconsistencies added
	 * @param attemptRepair TODO
	 * @param sourceTable
	 * @param sourcePrimaryKeyColumn TODO
	 * @param sourceColumn
	 * @param targetTablesAndColumns
	 */
	protected void checkCount(EntityManager em, ConsistencyCheckResult result, boolean attemptRepair, String sourceTable, String sourcePrimaryKeyColumn, String sourceColumn, String...targetTablesAndColumns) {
		List<Pair<String, String>> targets = new ArrayList<>();
		for (int i = 0; i < targetTablesAndColumns.length; i += 2) {
			String targetTable = targetTablesAndColumns[i];
			String targetColumn = targetTablesAndColumns[i + 1];
			targets.add(Pair.of(targetTable, targetColumn));
		}

		if (targets.isEmpty()) {
			return;
		}
		StringBuilder sqlBuilder = new StringBuilder(String.format("FROM %s source_table", sourceTable));
		StringBuilder whereBuilder = new StringBuilder();
		for (int i = 0; i < targets.size(); i++) {
			String targetTable = targets.get(i).getLeft();
			String targetColumn = targets.get(i).getRight();
			sqlBuilder.append(String.format(" LEFT JOIN %s target_table_%d ON source_table.%s = target_table_%d.%s", targetTable, i, sourceColumn, i, targetColumn));
			whereBuilder.append(String.format(" %s target_table_%d.%s IS NULL", i == 0 ? "WHERE" : "AND", i, targetColumn));
		}
		sqlBuilder.append(whereBuilder.toString());

		String countStatement = String.format("SELECT COUNT(*) %s", sqlBuilder.toString());
		String selectStatement = String.format("SELECT source_table.%s %s", sourcePrimaryKeyColumn, sqlBuilder.toString());

		Object countResult = em.createNativeQuery(countStatement).getSingleResult();
		if (countResult instanceof Number) {
			long count = Number.class.cast(countResult).longValue();
			if (count > 0) {
				InconsistencyInfoModel info = new InconsistencyInfoModel()
						.setDescription(String.format(
								"Table %s contains %d records, that reference records, which do not exist in tables %s",
								sourceTable, count, targets))
						.setSeverity(InconsistencySeverity.LOW).setRepairAction(RepairAction.DELETE);

				if (attemptRepair) {
					deleteStaleRecords(sourceTable, sourcePrimaryKeyColumn, selectStatement);
					info.setRepaired(true);
				}

				result.addInconsistency(info);
			}
		}
	}

	/**
	 * Delete stale records (in batches of max. 1000 records)
	 * @param em entity manager
	 * @param sourceTable name of the source table
	 * @param sourcePrimaryKeyColumn name of the primary key column in the source table (which is used to identify records to be deleted)
	 * @param selectStatement select statement which selects values of the primary key columns of stale records
	 */
	protected void deleteStaleRecords(String sourceTable, String sourcePrimaryKeyColumn, String selectStatement) {
		long sum = 0;
		int deletedEntries = 0;
		int limit = Math.min(1000, HibernateUtil.inQueriesLimit());
		do {
			deletedEntries = 0;
			try (Connection conn = db.noTx()) {
				Dialect dialect = db.getDatabaseConnector().getHibernateDialect();
				Limit sqllimit = new Limit();
				sqllimit.setMaxRows(limit);
				deletedEntries = conn.createStatement().executeUpdate("DELETE FROM " + sourceTable + " WHERE " + sourcePrimaryKeyColumn + " IN (" + dialect.getLimitHandler().processSql(selectStatement, sqllimit).replaceFirst("\\?", Integer.toString(limit)) + ")");
			} catch (Throwable e) {
				log.warn("Could not create own JDBC connection. Falling back to the provided EntityManager.", e);
				String deleteFrom = "delete from " + sourceTable + " where " + sourcePrimaryKeyColumn + " in :dbuuids "; 

				try (Tx tx = db.tx()) {
					EntityManager em = tx.<HibernateTx>unwrap().entityManager();
					jakarta.persistence.Query selectQuery = em.createNativeQuery(selectStatement).setMaxResults(limit);
	
					deletedEntries = 0;
					List<?> uuidsToDelete = selectQuery.getResultList();
	
					if (!uuidsToDelete.isEmpty()) {
						if (log.isDebugEnabled()) {
							log.debug("Delete {" + uuidsToDelete.size() + "} stale records from table {" + sourceTable + "}");
						}
						Query<?> query = em.createNativeQuery(deleteFrom).unwrap(Query.class);
						query.setParameterList("dbuuids", uuidsToDelete);
						deletedEntries = query.executeUpdate();					
					}
				}
			}
			sum += deletedEntries;
		} while(deletedEntries > 0);
		log.info("{} records removed from {}", sum, sourceTable);
	}

	@Override
	public String getName() {
		return "content";
	}
}

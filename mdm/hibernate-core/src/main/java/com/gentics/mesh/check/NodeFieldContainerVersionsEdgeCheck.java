package com.gentics.mesh.check;

import jakarta.persistence.EntityManager;

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
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * Test for checking consistency of "nodefieldcontainer_versions_edge"
 */
public class NodeFieldContainerVersionsEdgeCheck extends AbstractContentReferencingCheck {

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result, attemptRepair);
		return result;

	}

	@Override
	public String getName() {
		return "nodefieldcontainer_versions_edge";
	}

	/**
	 * Check for invalid record references
	 * @param tx transaction
	 * @param result check result
	 * @param attemptRepair 
	 */
	protected void checkInvalidRecordReferences(Tx tx, ConsistencyCheckResult result, boolean attemptRepair) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		SchemaDao schemaDao = tx.schemaDao();

		// check whether the table contains any entries, that reference inexistent contents

		// check for schemas
		Result<? extends Schema> schemas = schemaDao.findAll();
		for (Schema schema : schemas) {
			Iterable<? extends SchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (SchemaVersion version : versions) {
				if (attemptRepair) {
					repair(em, result, version, "thiscontentuuid", "thisversion_dbuuid");
					repair(em, result, version, "nextcontentuuid", "nextversion_dbuuid");
				} else {
					checkCount(em, result, version.getUuid(), "thiscontentuuid", "thisversion_dbuuid");
					checkCount(em, result, version.getUuid(), "nextcontentuuid", "nextversion_dbuuid");
				}
			}
		}
	}

	/**
	 * Repair the inconsistency with the removal of the offending edges.
	 * 
	 * @param em entity manager
	 * @param result check result, will get inconsistencies added
	 * @param version schema/microschema version
	 * @param contentRefColumn name of the column holding the content reference
	 * @param schemaVersionRefColumn name of the column holding the reference to the schema/microschema version
	 */
	protected void repair(EntityManager em, ConsistencyCheckResult result, SchemaVersion version, String contentRefColumn, String schemaVersionRefColumn) {
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
		String versionUuidParam = HibernateUtil.makeParamName(version);
		String contentTable = dc.getPhysicalTableName(version);
		String contentUuidColumn = dc.renderColumn(CommonContentColumn.DB_UUID);

		String sql = String.format(
				"DELETE FROM %s WHERE %s IN (SELECT ref.%s FROM %s ref LEFT JOIN %s content ON ref.%s = content.%s WHERE ref.%s = :%s AND content.%s IS NULL)", 
				refTableName, contentRefColumn, contentRefColumn, refTableName, contentTable, contentRefColumn, contentUuidColumn, schemaVersionRefColumn, versionUuidParam, contentUuidColumn);

		int countResult = em.createNativeQuery(sql)
				.setParameter(versionUuidParam, version.getId()).executeUpdate();

		if (countResult > 0) {
			long count = ((Number) countResult).longValue();
			if (count > 0) {
				InconsistencyInfoModel info = new InconsistencyInfoModel()
						.setDescription(String.format("Removed %d records from the table %s, that reference records, which do not exist in table %s", count, refTableName, contentTable))
						.setElementUuid(version.getUuid())
						.setSeverity(InconsistencySeverity.LOW)
						.setRepairAction(RepairAction.DELETE);
				result.addInconsistency(info);
			}
		}
	}
}

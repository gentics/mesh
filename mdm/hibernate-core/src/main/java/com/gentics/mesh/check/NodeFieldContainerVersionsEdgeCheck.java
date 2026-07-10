package com.gentics.mesh.check;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * Test for checking consistency of "nodefieldcontainer_versions_edge"
 */
public class NodeFieldContainerVersionsEdgeCheck extends AbstractRepairableContentReferencingCheck {

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
		Result<? extends HibSchema> schemas = schemaDao.findAll();
		for (HibSchema schema : schemas) {
			Iterable<? extends HibSchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (HibSchemaVersion version : versions) {
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
}

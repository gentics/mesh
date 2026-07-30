package com.gentics.mesh.check;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateTx;

/**
 * Test for checking consistency of "nodefieldcontainer"
 */
public class NodeFieldContainerCheck extends AbstractRepairableContentReferencingCheck {
	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result, attemptRepair);
		return result;

	}

	@Override
	public String getName() {
		return "nodefieldcontainer";
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
					repair(em, result, version, "contentuuid", "version_dbuuid");
				} else {
					checkCount(em, result, version.getUuid(), "contentuuid", "version_dbuuid");
				}
			}
		}
	}
}

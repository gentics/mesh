package com.gentics.mesh.check;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateTx;

/**
 * Test for checking consistency of "nodefieldcontainer"
 */
public class NodeFieldContainerCheck extends AbstractContentReferencingCheck {
	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result);
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
	 */
	protected void checkInvalidRecordReferences(Tx tx, ConsistencyCheckResult result) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		SchemaDao schemaDao = tx.schemaDao();

		// check whether the table contains any entries, that reference inexistent contents

		// check for schemas
		Result<? extends Schema> schemas = schemaDao.findAll();
		for (Schema schema : schemas) {
			Iterable<? extends SchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (SchemaVersion version : versions) {
				checkCount(em, result, version.getUuid(), "contentuuid", "version_dbuuid");
			}
		}
	}
}

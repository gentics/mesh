package com.gentics.mesh.check;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfoModel;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;

import jakarta.persistence.EntityManager;

/**
 * Abstract {@link ConsistencyCheck} implementation for tables which reference schemas or microschemas with column names containeruuid/containerversionuuid.
 */
public abstract class AbstractListItemTableCheck extends AbstractContentReferencingCheck {
	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result);
		return result;
	}

	/**
	 * Do the checks for all schemas and microschemas
	 * @param tx transaction
	 * @param result check result
	 */
	protected void checkInvalidRecordReferences(Tx tx, ConsistencyCheckResult result) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		DatabaseConnector dc = hibernateTx.data().getDatabaseConnector();
		SchemaDao schemaDao = tx.schemaDao();
		MicroschemaDao microschemaDao = tx.microschemaDao();

		// check whether the table contains any entries, that reference inexistent contents

		// check for schemas
		Result<? extends Schema> schemas = schemaDao.findAll();
		for (Schema schema : schemas) {
			Iterable<? extends SchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (SchemaVersion version : versions) {
				checkCount(em, result, version.getUuid(), "containeruuid", "containerversionuuid");
			}
		}

		// check for microschemas
		Result<? extends Microschema> microschemas = microschemaDao.findAll();
		for (Microschema microschema : microschemas) {
			Iterable<? extends MicroschemaVersion> versions = microschemaDao.findAllVersions(microschema);
			for (MicroschemaVersion version : versions) {
				checkCount(em, result, version.getUuid(), "containeruuid", "containerversionuuid");

				// micronodes might be referenced also with other columns
				for (Pair<String, String> ref : optMicroNodeReferences()) {
					checkCount(em, result, version.getUuid(), ref.getLeft(), ref.getRight());
				}
			}
		}

		// check optional other references
		List<Pair<String, String>> references = optValueReferencesToCheck();
		String refTable = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + getName();
		for (Pair<String, String> ref : references) {
			String refColumn = dc.identify(ref.getLeft(), false).render(dc.getHibernateDialect());
			String otherTable = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + ref.getRight();
			String otherRefColumn = dc.identify("dbuuid", false).render(dc.getHibernateDialect());
			String sql = String.format("SELECT COUNT(*) c FROM %s ref LEFT JOIN %s other ON ref.%s = other.%s WHERE other.%s IS NULL", refTable, otherTable, refColumn, otherRefColumn, otherRefColumn);

			Object countResult = em.createNativeQuery(sql).getSingleResult();

			if (countResult instanceof Number) {
				long count = ((Number) countResult).longValue();
				if (count > 0) {
					InconsistencyInfoModel info = new InconsistencyInfoModel()
							.setDescription(String.format("Table %s contains %d records, that reference records, which do not exist in table %s", refTableName, count, otherTable))
							.setSeverity(InconsistencySeverity.LOW)
							.setRepairAction(RepairAction.NONE);
					result.addInconsistency(info);
				}
			}
		}
	}

	/**
	 * Get the list of string pairs defining optional micronode references. Each
	 * pair must consist of the column names for the column referencing the
	 * microcontent (left) and the column referencing the microschema version
	 * (right)
	 * 
	 * @return list of column name pairs
	 */
	protected List<Pair<String, String>> optMicroNodeReferences() {
		return Collections.emptyList();
	}

	/**
	 * Get the list of string pairs defining optional other references. Each pair
	 * must consist of the column name (left) and the name of the target table
	 * (right)
	 * 
	 * @return list of column name/target table name pairs
	 */
	protected List<Pair<String, String>> optValueReferencesToCheck() {
		return Collections.emptyList();
	}
}

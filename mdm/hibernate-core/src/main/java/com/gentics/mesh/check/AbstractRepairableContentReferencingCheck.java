package com.gentics.mesh.check;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.util.HibernateUtil;

import jakarta.persistence.EntityManager;

/**
 * A content referencing check, which inconsistensies could be repaired.
 */
public abstract class AbstractRepairableContentReferencingCheck extends AbstractContentReferencingCheck {

	/**
	 * Repair the inconsistency with the removal of the offending edges.
	 * 
	 * @param em entity manager
	 * @param result check result, will get inconsistencies added
	 * @param version schema/microschema version
	 * @param contentRefColumn name of the column holding the content reference
	 * @param schemaVersionRefColumn name of the column holding the reference to the schema/microschema version
	 */
	protected void repair(EntityManager em, ConsistencyCheckResult result, HibSchemaVersion version, String contentRefColumn, String schemaVersionRefColumn) {
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
				InconsistencyInfo info = new InconsistencyInfo()
						.setDescription(String.format("Removed %d records from the table %s, that reference records, which do not exist in table %s", count, refTableName, contentTable))
						.setElementUuid(version.getUuid())
						.setSeverity(InconsistencySeverity.LOW)
						.setRepairAction(RepairAction.DELETE);
				result.addInconsistency(info);
			}
		}
	}
}

package com.gentics.mesh.check;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.EntityManager;

/**
 * Abstract {@link ConsistencyCheck} implementation for tables which reference records in one of the content tables.
 * {@link #getName()} must return the table name (without prefix)
 */
public abstract class AbstractContentReferencingCheck extends AbstractHibernateConsistencyCheck {
	/**
	 * full table name (including the prefix)
	 */
	protected String refTableName = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + getName();

	/**
	 * Check that there are not records referencing content records which do not exist
	 * @param em entity manager
	 * @param result check result, will get inconsistencies added
	 * @param versionUuid uuid of the schema/microschema version
	 * @param contentRefColumn name of the column holding the content reference
	 * @param schemaVersionRefColumn name of the column holding the reference to the schema/microschema version
	 */
	protected void checkCount(EntityManager em, ConsistencyCheckResult result, String versionUuid, String contentRefColumn, String schemaVersionRefColumn) {
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
		String versionUuidParam = HibernateUtil.makeParamName(versionUuid);
		String contentTable = dc.getPhysicalTableName(UUIDUtil.toJavaUuid(versionUuid));
		String contentUuidColumn = dc.renderColumn(CommonContentColumn.DB_UUID);

		String sql = String.format(
				"SELECT COUNT(1) c FROM %s ref LEFT JOIN %s content ON ref.%s = content.%s WHERE ref.%s = :%s AND content.%s IS NULL",
				refTableName, contentTable, contentRefColumn, contentUuidColumn, schemaVersionRefColumn, versionUuidParam, contentUuidColumn);

		Object countResult = em.createNativeQuery(sql)
				.setParameter(versionUuidParam, UUIDUtil.toJavaUuid(versionUuid)).getSingleResult();

		if (countResult instanceof Number) {
			long count = ((Number) countResult).longValue();
			if (count > 0) {
				InconsistencyInfo info = new InconsistencyInfo()
						.setDescription(String.format("Table %s contains %d records, that reference records, which do not exist in table %s", refTableName, count, contentTable))
						.setElementUuid(versionUuid)
						.setSeverity(InconsistencySeverity.LOW)
						.setRepairAction(RepairAction.NONE);
				result.addInconsistency(info);
			}
		}
	}
}

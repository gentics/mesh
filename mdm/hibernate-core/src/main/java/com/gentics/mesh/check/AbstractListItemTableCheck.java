package com.gentics.mesh.check;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.EntityManager;

/**
 * Abstract {@link ConsistencyCheck} implementation for tables which reference schemas or microschemas with column names containeruuid/containerversionuuid.
 */
public abstract class AbstractListItemTableCheck extends AbstractContentReferencingCheck {
	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		checkInvalidRecordReferences(tx, result, attemptRepair);
		return result;
	}

	@SuppressWarnings("unchecked")
	protected void findOrphanedListItems(EntityManager em, ConsistencyCheckResult result, HibFieldSchemaVersionElement<?,?,?,?,?> version, String contentRefColumn, String schemaVersionRefColumn, boolean attemptRepair) {
		if (getListFieldType() == null) {
			// This type does not support being a list item
			return;
		}
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
		String contentTable = dc.getPhysicalTableName(UUIDUtil.toJavaUuid(version.getUuid()));
		String uuidColumn = dc.renderColumn(CommonContentColumn.DB_UUID);

		List<String> listFields = version.getSchema().getFields().stream()
				.filter(f -> FieldTypes.valueByName(f.getType()).equals(FieldTypes.LIST) && FieldTypes.valueByName(((ListFieldSchema) f).getListType()).equals(getListFieldType()))
				.map(f -> f.getName())
				.collect(Collectors.toList());

		for (String field : listFields) {
			String fieldParam = HibernateUtil.makeParamName(field);
			String fieldColumn = dc.identify(field + "-list." + getListFieldType().name().toLowerCase()).render(dc.getHibernateDialect());
			String sql = String.format(
					"SELECT ref.%s c FROM %s ref LEFT JOIN %s content ON ref.%s = content.%s WHERE ref.fieldkey = :%s AND content.%s <> ref.listuuid",
					uuidColumn, refTableName, contentTable, contentRefColumn, uuidColumn, fieldParam, fieldColumn);

			List<UUID> orphanedListItems = em.createNativeQuery(sql)
					.setParameter(fieldParam, field).getResultList();

			if (orphanedListItems.size() > 0) {
				InconsistencyInfo info = new InconsistencyInfo()
						.setDescription(String.format("Table %s contains %d records, that were abandoned from the records of table %s", refTableName, orphanedListItems.size(), contentTable))
						.setElementUuid(version.getUuid())
						.setSeverity(InconsistencySeverity.LOW)
						.setRepairAction(RepairAction.DELETE);
				long deleted = 0;
				if (attemptRepair) {
					String delete = "DELETE FROM " + refTableName + " WHERE " + uuidColumn + " IN :" + fieldParam;
					deleted += SplittingUtils.splitAndCount(orphanedListItems, HibernateUtil.inQueriesLimitForSplitting(1), slice -> Long.valueOf(em.createNativeQuery(delete).setParameter(fieldParam, slice).executeUpdate()));
					info.setRepaired(deleted == orphanedListItems.size());
				}
				result.addInconsistency(info);
			}
		}
	}

	/**
	 * Get a {@link FieldTypes} instance of the list field type we currently process.
	 * 
	 * @return type or null, if the type is not supporting being the list item
	 */
	protected abstract FieldTypes getListFieldType();

	/**
	 * Do the checks for all schemas and microschemas
	 * @param tx transaction
	 * @param result check result
	 * @param attemptRepair 
	 */
	protected void checkInvalidRecordReferences(Tx tx, ConsistencyCheckResult result, boolean attemptRepair) {
		HibernateTx hibernateTx = (HibernateTx) tx;
		EntityManager em = hibernateTx.entityManager();
		DatabaseConnector dc = hibernateTx.data().getDatabaseConnector();
		SchemaDao schemaDao = tx.schemaDao();
		MicroschemaDao microschemaDao = tx.microschemaDao();

		// check whether the table contains any entries, that reference inexistent contents

		// check for schemas
		Result<? extends HibSchema> schemas = schemaDao.findAll();
		for (HibSchema schema : schemas) {
			Iterable<? extends HibSchemaVersion> versions = schemaDao.findAllVersions(schema);
			for (HibSchemaVersion version : versions) {
				checkCount(em, result, version.getUuid(), "containeruuid", "containerversionuuid");
				findOrphanedListItems(em, result, version, "containeruuid", "containerversionuuid", attemptRepair);
			}
		}

		// check for microschemas
		Result<? extends HibMicroschema> microschemas = microschemaDao.findAll();
		for (HibMicroschema microschema : microschemas) {
			Iterable<? extends HibMicroschemaVersion> versions = microschemaDao.findAllVersions(microschema);
			for (HibMicroschemaVersion version : versions) {
				checkCount(em, result, version.getUuid(), "containeruuid", "containerversionuuid");

				// micronodes might be referenced also with other columns
				for (Pair<String, String> ref : optMicroNodeReferences()) {
					checkCount(em, result, version.getUuid(), ref.getLeft(), ref.getRight());
					findOrphanedListItems(em, result, version, ref.getLeft(), ref.getRight(), attemptRepair);
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
					InconsistencyInfo info = new InconsistencyInfo()
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

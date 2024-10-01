package com.gentics.mesh.liquibase.changelog.v1_0_4;

import static com.gentics.mesh.contentoperation.CommonContentColumn.DB_UUID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.liquibase.LiquibaseStartupContext;
import com.gentics.mesh.util.UUIDUtil;

import liquibase.Scope;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.logging.Logger;
import liquibase.resource.ResourceAccessor;

/**
 * This change copies editor data from the field container onto its corresponding edge, 
 * to allow filtering over editor data without a need to map a distinct schema version. 
 * 
 * @author plyhun
 *
 */
public class CopyEditorTrackingIntoEdge implements CustomTaskChange {

	private final ValidationErrors errors = new ValidationErrors("CopyEditorTrackingIntoEdge"); 

	private boolean isUuidSupported;

	@Override
	public String getConfirmationMessage() {
		return "CopyEditorTrackingIntoEdge confirmed";
	}

	@Override
	public void setUp() throws SetupException {
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {		
	}

	@Override
	public ValidationErrors validate(Database database) {
		return errors;
	}

	private UUID uuidFromRs(ResultSet rs, int column) throws SQLException {
		if (isUuidSupported) {
			return rs.getObject(column, UUID.class);
		} else {
			byte[] bytes = rs.getBytes(column);
			return UUIDUtil.toJavaUuid(bytes);
		}
	}

	private boolean isUuidTypeSupported(Database database) {
		String vendorName = database.getDatabaseProductName().toLowerCase();
		return vendorName.contains("postgres");
	}

	@Override
	public void execute(Database database) throws CustomChangeException {
		Logger log = Scope.getCurrentScope().getLog(getClass());

		String containerTableName;
		try {
			containerTableName = LiquibaseStartupContext.getConnectorIfStartup().maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get();
		} catch (Throwable e1) {
			errors.addWarning("Change is started via migration script or unit test, i.e. the initial DB is empty and needs no editor tracking copy. Exiting.");
			return;
		}

		log.info("Starting EditorTracking injection into " + containerTableName);
		JdbcConnection conn = (JdbcConnection) database.getConnection();
		isUuidSupported = isUuidTypeSupported(database);

		try {
			Set<Pair<UUID, Object>> versionUuids = getVersionUuids(conn, containerTableName);

			int tableCounter = 0;
			for (Pair<UUID, Object> pair : versionUuids) {
				UUID versionUuid = pair.getLeft();
				Object dbValue = pair.getRight();
				tableCounter++;
				log.info(String.format("Start %d/%d content tables", tableCounter, versionUuids.size()));

				String versionString = UUIDUtil.toShortUuid(versionUuid);
				String contentTableName = MeshTablePrefixStrategy.TABLE_NAME_PREFIX + MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX + versionString;

				try (PreparedStatement pst = conn.prepareStatement(String.format(
						"UPDATE %s SET edited = (SELECT %s FROM %s WHERE %s.contentuuid = %s.%s) WHERE version_dbuuid = ?",
						containerTableName, LiquibaseStartupContext.getConnectorIfStartup().renderColumnUnsafe("edited", false), contentTableName, containerTableName, contentTableName, LiquibaseStartupContext.getConnectorIfStartup().renderColumn(DB_UUID)))) {
					pst.setObject(1, dbValue);
					int updatedRows = pst.executeUpdate();
					log.info(String.format("%d updated rows for column 'edited'", updatedRows));
				}

				try (PreparedStatement pst = conn.prepareStatement(String.format(
						"UPDATE %s SET editor_dbuuid = (SELECT %s FROM %s WHERE %s.contentuuid = %s.%s) WHERE version_dbuuid = ?",
						containerTableName, LiquibaseStartupContext.getConnectorIfStartup().renderColumnUnsafe("editor_dbuuid", false), contentTableName, containerTableName, contentTableName, LiquibaseStartupContext.getConnectorIfStartup().renderColumn(DB_UUID)))) {
					pst.setObject(1, dbValue);
					int updatedRows = pst.executeUpdate();
					log.info(String.format("%d updated rows for column 'editor_dbuuid'", updatedRows));
				}

				log.info(String.format("Done %d/%d content tables", tableCounter, versionUuids.size()));
			}
		} catch (DatabaseException | SQLException e) {
			throw new CustomChangeException(e);
		}
	}

	/**
	 * Get all used version UUIDs (UUIDs of schema versions) from the nodefieldcontainer table
	 * @param conn connection
	 * @param containerTableName name of the nodefieldcontainer table
	 * @return set of pairs containing the version as UUID and as object returned by the database
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	private Set<Pair<UUID, Object>> getVersionUuids(JdbcConnection conn, String containerTableName) throws DatabaseException, SQLException {
		Set<Pair<UUID, Object>> versionUuids = new HashSet<>();
		try (PreparedStatement pst = conn.prepareStatement("SELECT DISTINCT version_dbuuid FROM " + containerTableName)) {
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					UUID uuid = uuidFromRs(rs, 1);
					Object value = rs.getObject(1);
					versionUuids.add(Pair.of(uuid, value));
				}
			}
		}
		return versionUuids;
	}
}

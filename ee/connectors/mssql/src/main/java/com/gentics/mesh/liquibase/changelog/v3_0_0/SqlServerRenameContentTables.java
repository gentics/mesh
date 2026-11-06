package com.gentics.mesh.liquibase.changelog.v3_0_0;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.util.HibernateUtil;
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
 * Hibernate 6.x started supporting native UUID types. 
 * All of a sudden, ones of Microsoft SQL Server appear to differ from their binary representation, so some tuning is required to access the content tables.
 */
public class SqlServerRenameContentTables implements CustomTaskChange {

	private final ValidationErrors errors = new ValidationErrors("SqlServerRenameContentTables"); 

	@Override
	public String getConfirmationMessage() {
		return "SqlServerRenameContentTables confirmed";
	}

	@Override
	public void setUp() throws SetupException {}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {}

	@Override
	public ValidationErrors validate(Database database) {
		return errors;
	}

	@Override
	public void execute(Database database) throws CustomChangeException {
		Logger log = Scope.getCurrentScope().getLog(getClass());
		Scope.getCurrentScope().getLog(getClass()).info("Starting SqlServerRenameContentTables");

		JdbcConnection conn = (JdbcConnection) database.getConnection();

		Set<String> existingTables = HibernateUtil.getContentTables(conn.getUnderlyingConnection(), MeshTablePrefixStrategy.TABLE_NAME_PREFIX + MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX);
		try (
			PreparedStatement select = conn.prepareStatement("select cast(dbuuid as binary(16)), dbuuid from mesh_schemaversion union all select cast(dbuuid as binary(16)), dbuuid from mesh_microschemaversion"); 
			ResultSet selectRs = select.executeQuery();
		) {
			while (selectRs.next()) {
				byte[] uuidBytes = selectRs.getBytes(1);
				UUID oldUuid = UUIDUtil.toJavaUuid(uuidBytes);
				UUID uuidTyped = selectRs.getObject(2, UUID.class);
				String newTableString = "mesh_content_" + UUIDUtil.toShortUuid(uuidTyped);
				if (existingTables.contains(newTableString)) {
					log.info("Inconsistent or already processed: " + oldUuid);
					errors.addWarning("Inconsistent or already processed: " + oldUuid);
					continue;
				}
				try (PreparedStatement rename = conn.prepareStatement(String.format("sp_rename mesh_content_%s, %s", UUIDUtil.toShortUuid(oldUuid), newTableString))) {
					rename.execute();
				}
			}
		} catch (DatabaseException | SQLException e) {
			throw new CustomChangeException(e);
		}
	}
}

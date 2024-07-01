package com.gentics.mesh.liquibase.changelog.v1_0_4;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.liquibase.LiquibaseStartupContext;

import liquibase.Scope;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * The whole custom task is required here because there is no stable SQL syntax of using UPDATE with aliases.
 * This change is not performant, but fortunately occurs rarely, so no complex perf refactoring is applied.
 * The goal of this change is to fix the reference type from FIELD to MICRONODE where the edge actually points to a micronode. 
 * 
 * @author plyhun
 *
 */
public class FixMicronodeToNodeReferences implements CustomTaskChange {

	private final ValidationErrors errors = new ValidationErrors("FixMicronodeToNodeReferences"); 

	@Override
	public String getConfirmationMessage() {
		return "FixMicronodeToNodeReferences confirmed";
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

	@Override
	public void execute(Database database) throws CustomChangeException {
		String nodeEdgeTableName;
		try {
			nodeEdgeTableName = LiquibaseStartupContext.getConnectorIfStartup().maybeGetPhysicalTableName(HibNodeFieldEdgeImpl.class).get();
		} catch (Exception e1) {
			errors.addWarning("Change is started via migration script or unit test, i.e. the initial DB is empty and needs no micronode reference fix. Exiting.");
			return;
		}
		Scope.getCurrentScope().getLog(getClass()).info("Starting FixMicronodeToNodeReferences over " + nodeEdgeTableName);
		JdbcConnection conn = (JdbcConnection) database.getConnection();
		try (ResultSet rs = conn.prepareStatement("SELECT ref.dbuuid FROM " + nodeEdgeTableName + " ref "
				+ " WHERE ref.containertype = 'FIELD' "
				+ " AND NOT EXISTS (select 1 from mesh_schemaversion ver where ver.dbuuid = ref.containerversionuuid) "
				+ " AND EXISTS (select 1 from mesh_microschemaversion ver where ver.dbuuid = ref.containerversionuuid)").executeQuery()) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				PreparedStatement updateContentStmt = conn.prepareStatement("UPDATE " + nodeEdgeTableName + " SET containertype = 'MICRONODE' WHERE dbuuid = ? ");
				updateContentStmt.setBytes(1, uuid);
				updateContentStmt.executeUpdate();
				updateContentStmt.close();
			}
		} catch (DatabaseException | SQLException e) {
			throw new CustomChangeException(e);
		}	
	}
}

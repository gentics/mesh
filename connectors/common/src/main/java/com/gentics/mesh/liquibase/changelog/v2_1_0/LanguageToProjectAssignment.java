package com.gentics.mesh.liquibase.changelog.v2_1_0;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import liquibase.Scope;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class LanguageToProjectAssignment implements CustomTaskChange {

	private final ValidationErrors errors = new ValidationErrors("LanguageToProjectAssignment"); 

	@Override
	public String getConfirmationMessage() {
		return "LanguageToProjectAssignment confirmed";
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
		Scope.getCurrentScope().getLog(getClass()).info("Starting LanguageToProjectAssignment");
		JdbcConnection conn = (JdbcConnection) database.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(
				"insert into mesh_project_language (project_dbuuid, languages_dbuuid) "
				+ "select distinct project.dbuuid, elanguage.dbuuid from mesh_nodefieldcontainer edge "
						+ " inner join mesh_language elanguage on elanguage.languagetag = edge.languagetag "
						+ " inner join mesh_node node on node.dbuuid = edge.node_dbuuid "
						+ " inner join mesh_project project on project.dbuuid = node.project_dbuuid "
						+ " left outer join mesh_project_language planguage on planguage.project_dbuuid = project.dbuuid"
						+ " where planguage.languages_dbuuid IS NULL")) {
			long count = stmt.executeUpdate();
			Scope.getCurrentScope().getLog(getClass()).info(count + " records created");
		} catch (DatabaseException | SQLException e) {
			throw new CustomChangeException(e);
		};
	}

}

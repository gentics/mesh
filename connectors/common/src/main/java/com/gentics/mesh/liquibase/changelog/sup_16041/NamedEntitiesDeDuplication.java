package com.gentics.mesh.liquibase.changelog.sup_16041;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.util.UUIDUtil;

import liquibase.Scope;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class NamedEntitiesDeDuplication implements CustomTaskChange {

	private final ValidationErrors errors = new ValidationErrors("NamedEntitiesDeDuplication"); 

	@Override
	public String getConfirmationMessage() {
		return "NamedEntitiesDeDuplication confirmed";
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
		Scope.getCurrentScope().getLog(getClass()).info("Starting NamedEntitiesDeDuplication");
		executeUnrooted(database);
		executeRooted(database);
	}

	@SuppressWarnings("unchecked")
	private void executeRooted(Database database) throws CustomChangeException {
		Pair<String, String>[] namedEntities = new Pair[] { Pair.of("mesh_tagfamily", "project_dbuuid"), Pair.of("mesh_tag", "tagfamily_dbuuid"), Pair.of("mesh_branch", "project_dbuuid") };
		JdbcConnection conn = (JdbcConnection) database.getConnection();
		for (Pair<String, String> entity : namedEntities) {
			String entityTable = entity.getKey();
			String entityRootUuidColumn = entity.getValue(); 
			try (ResultSet rs = conn.prepareStatement("SELECT ref.name, ref." + entityRootUuidColumn + ", COUNT(*) FROM " + entityTable + " ref GROUP BY ref.name, ref." + entityRootUuidColumn).executeQuery()) {
				while (rs.next()) {
					String name = rs.getString(1);
					byte[] rootUuid = rs.getBytes(2);
					long count = rs.getLong(3);
					if (count < 2) {
						continue;
					}
					Scope.getCurrentScope().getLog(getClass()).info("Deduplicating " + name + " / " + UUIDUtil.toShortUuid(UUIDUtil.toJavaUuid(rootUuid)) + " of " + entityTable);
					PreparedStatement readStmt = conn.prepareStatement("SELECT ref.dbuuid FROM " + entityTable + " ref WHERE ref.name = ? AND ref." + entityRootUuidColumn + " = ? ORDER BY ref.created ASC");
					readStmt.setString(1, name);
					readStmt.setBytes(2, rootUuid);
					try (ResultSet rs1 = readStmt.executeQuery()) {
						int total = 0;
						while (rs1.next()) {
							total++;
							if (total < 2) {
								continue;
							}
							byte[] uuid = rs1.getBytes(1);
							PreparedStatement updateContentStmt = conn.prepareStatement("UPDATE " + entityTable + " SET name = ? WHERE dbuuid = ? ");
							updateContentStmt.setString(1, "dedup_" + name + "_" + UUIDUtil.toShortUuid(UUIDUtil.toJavaUuid(uuid)));
							updateContentStmt.setBytes(2, uuid);
							updateContentStmt.executeUpdate();
							updateContentStmt.close();
						}
					}
				}
			} catch (DatabaseException | SQLException e) {
				throw new CustomChangeException(e);
			}	 
		}
	}

	private void executeUnrooted(Database database) throws CustomChangeException {
		String[] namedEntities = { "mesh_user", "mesh_group", "mesh_language", "mesh_role", "mesh_project", "mesh_schema", "mesh_microschema" };
		JdbcConnection conn = (JdbcConnection) database.getConnection();
		for (String entityTable : namedEntities) {
			try (ResultSet rs = conn.prepareStatement("SELECT ref.name, COUNT(*) FROM " + entityTable + " ref GROUP BY ref.name").executeQuery()) {
				while (rs.next()) {
					String name = rs.getString(1);
					long count = rs.getLong(2);
					if (count < 2) {
						continue;
					}
					Scope.getCurrentScope().getLog(getClass()).info("Deduplicating " + name + " of " + entityTable);
					PreparedStatement readStmt = conn.prepareStatement("SELECT ref.dbuuid FROM " + entityTable + " ref WHERE ref.name = ? ORDER BY ref.created ASC");
					readStmt.setString(1, name);
					try (ResultSet rs1 = readStmt.executeQuery()) {
						int total = 0;
						while (rs1.next()) {
							total++;
							if (total < 2) {
								continue;
							}
							byte[] uuid = rs1.getBytes(1);
							PreparedStatement updateContentStmt = conn.prepareStatement("UPDATE " + entityTable + " SET name = ? WHERE dbuuid = ? ");
							updateContentStmt.setString(1, "dedup_" + name + "_" + UUIDUtil.toShortUuid(UUIDUtil.toJavaUuid(uuid)));
							updateContentStmt.setBytes(2, uuid);
							updateContentStmt.executeUpdate();
							updateContentStmt.close();
						}
					}
				}
			} catch (DatabaseException | SQLException e) {
				throw new CustomChangeException(e);
			}	 
		}
	}
}

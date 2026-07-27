package com.gentics.mesh.liquibase.changelog.v3_3_0;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.util.UUIDUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.CustomPreconditionErrorException;
import liquibase.exception.CustomPreconditionFailedException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.precondition.CustomPrecondition;
import liquibase.resource.ResourceAccessor;

/**
 * Custom change task that migrates API Tokens stored in the user table to their
 * own entities
 */
public class MigrateUserAPITokens implements CustomTaskChange, CustomPrecondition {
	/**
	 * Name of the system property, which will omit this change, when set to "true"
	 */
	public final static String OMIT_PRECONDITION = "MigrateUserAPITokens.omit";

	/**
	 * Create a random UUID and return it in the same format as the given uuid
	 * @param uuid example UUID
	 * @return random UUId in the same format as the example
	 */
	protected static Object createRandomUuid(Object uuid) throws CustomChangeException {
		String randomUUID = UUIDUtil.randomUUID();
		if (uuid instanceof byte[]) {
			return UUIDUtil.toBytes(UUIDUtil.toJavaUuid(randomUUID));
		} else if (uuid instanceof UUID) {
			return UUIDUtil.toJavaUuid(randomUUID);
		} else if (uuid instanceof String) {
			return UUIDUtil.toFullUuid(randomUUID);
		} else {
			throw new CustomChangeException("UUID has unknown type %s".formatted(uuid.getClass()));
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "MigrateUserAPITokens confirmed";
	}

	@Override
	public void setUp() throws SetupException {
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
	}

	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}

	@Override
	public void execute(Database database) throws CustomChangeException {
		JdbcConnection conn = (JdbcConnection) database.getConnection();

		List<Triple<Object, String, Long>> tokens = new ArrayList<>();
		try (PreparedStatement pst = conn.prepareStatement(
				"SELECT dbuuid, apitokenid, apitokenissuetimestamp FROM mesh_user")) {
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					Object uuid = rs.getObject("dbuuid");
					String apiTokenId = rs.getString("apitokenid");
					Long apiTokenIssueTimestamp = rs.getLong("apitokenissuetimestamp");

					if (StringUtils.isNotBlank(apiTokenId)) {
						tokens.add(Triple.of(uuid, apiTokenId, apiTokenIssueTimestamp));
					}
				}
			}
		} catch (DatabaseException | SQLException e) {
			throw new CustomChangeException(e);
		}

		if (!tokens.isEmpty()) {
			try (PreparedStatement pst = conn.prepareStatement(
					"INSERT INTO mesh_apitoken (dbuuid, dbversion, name, user_dbuuid, tokenid, issued, lastused, expires) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
				for (Triple<Object, String, Long> token : tokens) {
					Object uuid = createRandomUuid(token.getLeft());
					Timestamp issued = Timestamp.from(Instant.ofEpochMilli(token.getRight()));
					Timestamp zero = Timestamp.from(Instant.ofEpochMilli(0));

					pst.setObject(1, uuid);                 // dbuuid
					pst.setLong(2, 1);                      // dbversion
					pst.setString(3, "Migrated API Token"); // name
					pst.setObject(4, token.getLeft());      // user_dbuuid
					pst.setString(5, token.getMiddle());    // tokenid
					pst.setTimestamp(6, issued);            // issued
					pst.setTimestamp(7, zero);              // lastused
					pst.setTimestamp(8, zero);              // expires

					pst.addBatch();
				}

				pst.executeBatch();
			} catch (DatabaseException | SQLException e) {
				throw new CustomChangeException(e);
			}
		}

	}

	@Override
	public void check(Database database) throws CustomPreconditionFailedException, CustomPreconditionErrorException {
		if ("true".equals(System.getProperty(OMIT_PRECONDITION))) {
			throw new CustomPreconditionFailedException("Skipped in test");
		}
	}
}

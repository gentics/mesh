package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Condition;
import org.hibernate.Session;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.APITokenDao;
import com.gentics.mesh.core.data.user.HibAPITokenData;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.liquibase.LiquibaseConnectionProvider;
import com.gentics.mesh.liquibase.LiquibaseLogService;
import com.gentics.mesh.liquibase.LiquibaseStartupContext;
import com.gentics.mesh.liquibase.LiquibaseUIService;
import com.gentics.mesh.liquibase.changelog.v3_3_0.MigrateUserAPITokens;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestInitializer;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.TokenUtil;

import jakarta.persistence.EntityManager;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;

/**
 * Test for automatic migration of user API Tokens via a custom liqubase change
 */
@MeshTestSetting(testSize = FULL, startServer = false, initializer = APITokenMigrationTest.class)
public class APITokenMigrationTest extends AbstractMeshTest implements MeshTestInitializer {
	public final static List<String> usersWithApiTokens = List.of("joe1", "guest");

	@Override
	public void init() {
		// first set the system property to omit the migration
		System.setProperty(MigrateUserAPITokens.OMIT_PRECONDITION, "true");
	}

	@Test
	public void testMigration() throws Exception {
		Map<String, Pair<String, Long>> apiTokens = new HashMap<>();

		// prepare old API Tokens
		tx(tx -> {
			HibernateTx hTx = tx.unwrap();

			EntityManager em = hTx.entityManager();
			String userTableName = hTx.data().getDatabaseConnector().maybeGetPhysicalTableName(HibUserImpl.class).get();

			for (String userName : usersWithApiTokens) {
				String userUuid = users().get(userName).getUuid();
				String tokenId = TokenUtil.randomToken();
				long issuedAt = RandomUtils.insecure().randomLong(
						Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
						Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());
				apiTokens.put(userUuid, Pair.of(tokenId, issuedAt));
				em.createNativeQuery("UPDATE %s SET apitokenid = ?, apitokenissuetimestamp = ? WHERE name = ?".formatted(userTableName))
					.setParameter(1, tokenId)
					.setParameter(2, issuedAt)
					.setParameter(3, userName)
					.executeUpdate();
			}
		});

		// reset the system property to no longer omit the migration
		System.setProperty(MigrateUserAPITokens.OMIT_PRECONDITION, "false");

		// let liquibase run again to do the migration
		tx(tx -> {
			HibernateTx hTx = tx.unwrap();
			EntityManager em = hTx.entityManager();
			LiquibaseConnectionProvider liquibaseConnectionProvider = new LiquibaseConnectionProvider(
					hTx.data().options(), hTx.data().getDatabaseConnector());

			Session session = (Session) em.getDelegate();
			session.doWork(connection -> {
				try {
					Scope.child(Map.of(Scope.Attr.ui.name(), new LiquibaseUIService(), Scope.Attr.logService.name(),
							new LiquibaseLogService()), () -> {
						try (LiquibaseStartupContext lbctx = liquibaseConnectionProvider.getLiquibase(connection)) {
							Liquibase liquibase = lbctx.liquibase();
							liquibase.update((Contexts) null);
						}
					});
				} catch (Exception e) {
					log.error("Database migration failed", e);
					throw new RuntimeException("Database migration failed", e);
				} finally {
					em.close();
				}
			});
		});

		// check whether tokens have been migrated
		for (HibUser user : users().values()) {
			List<? extends HibAPITokenData> tokens = tx(tx -> {
				InternalActionContext ac = mockActionContext();
				PagingParameters paging = new PagingParametersImpl();

				APITokenDao apiTokenDao = tx.apiTokenDao();
				return apiTokenDao.findAll(ac, user, paging).getWrappedList();
			});

			if (apiTokens.containsKey(user.getUuid())) {
				String tokenId = apiTokens.get(user.getUuid()).getLeft();
				Long issuedAt = apiTokens.get(user.getUuid()).getRight();
				Condition<HibAPITokenData> tokenIdCondition = new Condition<HibAPITokenData>(
						token -> Strings.CS.equals(token.getTokenId(), tokenId), "Correct tokenId");
				Condition<HibAPITokenData> issuedAtCondition = new Condition<HibAPITokenData>(
						token -> Objects.equals(token.getIssuedTimestamp(), issuedAt), "Correct issue date");
				Condition<HibAPITokenData> nameCondition = new Condition<HibAPITokenData>(
						token -> Strings.CS.equals(token.getName(), "Migrated API Token"), "Correct name");
				Condition<HibAPITokenData> expiresCondition = new Condition<HibAPITokenData>(
						token -> Objects.equals(token.getExpiresTimestamp(), 0L), "Correct expiration date");

				assertThat(tokens).as("Tokens for %s".formatted(user.getName()))
					.hasSize(1)
					.areExactly(1, tokenIdCondition)
					.areExactly(1, issuedAtCondition)
					.areExactly(1, nameCondition)
					.areExactly(1, expiresCondition);
			} else {
				assertThat(tokens).as("Tokens for %s".formatted(user.getName())).isEmpty();
			}
		}
	}
}

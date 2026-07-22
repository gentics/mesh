package com.gentics.mesh.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.APITokenDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.user.HibAPITokenData;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.TokenUtil;

/**
 * Test cases for handling of {@link HibAPITokenData} instances
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class APITokenTest extends AbstractMeshTest {
	/**
	 * Test creation
	 */
	@Test
	public void testCreate() {
		String tokenId = TokenUtil.randomToken();
		HibAPITokenData tokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.create(user(), "Test Token", tokenId, null);
		});
		assertThat(tokenData).as("Created API Token")
			.isNotNull()
			.hasFieldOrProperty("uuid")
			.hasFieldOrPropertyWithValue("name", "Test Token")
			.hasFieldOrPropertyWithValue("user.username", "joe1")
			.hasFieldOrPropertyWithValue("tokenId", tokenId)
			.hasFieldOrPropertyWithValue("expiresTimestamp", null)
			.hasFieldOrPropertyWithValue("lastUsed", null)
			.hasFieldOrPropertyWithValue("valid", true);
	}

	/**
	 * Test creating an expired token
	 */
	@Test
	public void testCreatedExpired() {
		String tokenId = TokenUtil.randomToken();
		Instant expires = Instant.now().minus(1, ChronoUnit.MINUTES);

		HibAPITokenData tokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.create(user(), "Expired Token", tokenId, expires.toEpochMilli());
		});

		assertThat(tokenData).as("Created API Token")
			.isNotNull()
			.hasFieldOrPropertyWithValue("expiresTimestamp", expires.toEpochMilli())
			.hasFieldOrPropertyWithValue("lastUsed", null)
			.hasFieldOrPropertyWithValue("valid", false);
	}

	/**
	 * Test creating a token that will expired
	 */
	@Test
	public void testCreatedWillExpire() {
		String tokenId = TokenUtil.randomToken();
		Instant expires = Instant.now().plus(1, ChronoUnit.MINUTES);

		HibAPITokenData tokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.create(user(), "Expired Token", tokenId, expires.toEpochMilli());
		});

		assertThat(tokenData).as("Created API Token")
			.isNotNull()
			.hasFieldOrPropertyWithValue("expiresTimestamp", expires.toEpochMilli())
			.hasFieldOrPropertyWithValue("lastUsed", null)
			.hasFieldOrPropertyWithValue("valid", true);
	}

	/**
	 * Test loading by uuid
	 */
	@Test
	public void testLoadByUuid() {
		String tokenId = TokenUtil.randomToken();
		HibAPITokenData tokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.create(user(), "Test Token", tokenId, null);
		});

		HibAPITokenData reloadedTokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.findByUuid(user(), tokenData.getUuid());
		});
		assertThat(reloadedTokenData).as("Reloaded API Token")
			.isNotNull()
			.isEqualTo(tokenData);

		tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			Map<String, HibUser> users = new HashMap<>(users());
			users.remove(user().getUsername());
			assertThat(users).as("Map of other users")
				.isNotEmpty()
				.doesNotContainValue(user());

			for (HibUser user : users.values()) {
				assertThat(apiTokenDao.findByUuid(user, tokenData.getUuid())).as("Token fetched for other user").isNull();
			}
		});
	}

	/**
	 * Test loading by tokenId
	 */
	@Test
	public void testLoadByTokenId() {
		String tokenId = TokenUtil.randomToken();
		HibAPITokenData tokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.create(user(), "Test Token", tokenId, null);
		});

		HibAPITokenData reloadedTokenData = tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			return apiTokenDao.findByTokenId(user(), tokenId);
		});
		assertThat(reloadedTokenData).as("Reloaded API Token")
			.isNotNull()
			.isEqualTo(tokenData);

		tx(tx -> {
			APITokenDao apiTokenDao = tx.apiTokenDao();
			Map<String, HibUser> users = new HashMap<>(users());
			users.remove(user().getUsername());
			assertThat(users).as("Map of other users")
				.isNotEmpty()
				.doesNotContainValue(user());

			for (HibUser user : users.values()) {
				assertThat(apiTokenDao.findByTokenId(user, tokenId)).as("Token fetched for other user").isNull();
			}
		});
	}

	/**
	 * Test loading the list of API Tokens for a user
	 */
	@Test
	public void testList() {
		int numTokens = 100;
		Set<HibAPITokenData> tokens = new HashSet<>();
		for (int i = 0; i < numTokens; i++) {
			String name = "Test Token #%d".formatted(i);
			tokens.add(tx(tx -> {
				return tx.apiTokenDao().create(user(), name, TokenUtil.randomToken(), null);
			}));

			String foreignName = "Foreign Test Token #%d".formatted(i);
			tx(tx -> {
				tx.apiTokenDao().create(users().get("admin"), foreignName, TokenUtil.randomToken(), null);
			});
		}

		Page<? extends HibAPITokenData> list = tx(tx -> {
			InternalActionContext ac = mockActionContext();
			return tx.apiTokenDao().findAll(ac, user(), new PagingParametersImpl(1, 1000L));
		});

		assertThat(list).as("List of API Tokens").hasSameElementsAs(tokens);
	}
}

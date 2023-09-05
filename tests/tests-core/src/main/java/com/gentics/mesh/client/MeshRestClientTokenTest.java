package com.gentics.mesh.client;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.PROJECT;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;

/**
 * Test cases for refreshing the login token in the MeshRestClient
 */
@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = true, customOptionChanger = MeshRestClientTokenTest.SetTokenExpirationTime.class)
public class MeshRestClientTokenTest extends AbstractMeshTest {
	/**
	 * Token expiration time in seconds
	 */
	public final static int TOKEN_EXPIRATION_TIME_SECONDS = 5;

	/**
	 * API Token of the test user
	 */
	private String testUserApiToken;

	/**
	 * Username of the test user
	 */
	private String username;

	/**
	 * Password of the test user
	 */
	private String password;

	@Before
	public void setUp() throws Exception {
		String userUuid = TestDataProvider.getInstance().getUserInfo().getUserUuid();
		testUserApiToken = testContext.getHttpClient().issueAPIToken(userUuid).blockingGet().getToken();

		UserInfo userInfo = TestDataProvider.getInstance().getUserInfo();
		username = tx(tx -> {
			return userInfo.getUser().getUsername();
		});
		password = userInfo.getPassword();

		client().logout().blockingGet();
	}

	/**
	 * Test using login credentials
	 * @throws Exception
	 */
	@Test
	public void testLogin() throws Exception {
		client().setLogin(username, password).login().blockingGet();

		// now get "me" for 10 seconds (once every second). This will fail, when the token expires
		Flowable.intervalRange(0, 10, 0, 1, TimeUnit.SECONDS).flatMapSingle(v -> {
			return client().me().toSingle();
		}).doOnNext(response -> {
			// assert that we still are the correct user
			assertThat(response).hasName(username);
		}).blockingSubscribe();
	}

	/**
	 * Test using the API Token
	 * @throws Exception
	 */
	@Test
	public void testApiToken() throws Exception {
		UserInfo userInfo = TestDataProvider.getInstance().getUserInfo();
		String username = tx(tx -> {
			return userInfo.getUser().getUsername();
		});
		client().setAPIKey(testUserApiToken);

		// now get "me" for 10 seconds (once every second). This will fail, when the token expires
		Flowable.intervalRange(0, 10, 0, 1, TimeUnit.SECONDS).flatMapSingle(v -> {
			return client().me().toSingle();
		}).doOnNext(response -> {
			// assert that we still are the correct user
			assertThat(response).hasName(username);
			// assert that we still use the API Token
			assertThat(client().getAPIKey()).as("API Token").isEqualTo(testUserApiToken);
		}).blockingSubscribe();
	}

	/**
	 * Test that the token expires if waiting too long
	 * @throws Exception
	 */
	@Test
	public void testExpiration() throws Exception {
		client().setLogin(username, password).login().blockingGet();

		// wait 1 second longer than the expiration time
		Thread.sleep((TOKEN_EXPIRATION_TIME_SECONDS + 1) * 1000);

		call(() -> client().me(), HttpResponseStatus.UNAUTHORIZED);
	}

	/**
	 * Implementation of {@link MeshOptionChanger} which sets the token expiration time to 5 seconds
	 */
	public static class SetTokenExpirationTime implements MeshOptionChanger {
		@Override
		public void change(MeshOptions options) {
			// set the login token to expire after 5 seconds
			options.getAuthenticationOptions().setTokenExpirationTime(TOKEN_EXPIRATION_TIME_SECONDS);
		}
	}
}

package com.gentics.mesh.core.user;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Tries various endpoints in an attempt to enumerate users in Mesh.
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class UsernameEnumerationTest extends AbstractMeshTest {
	@Before
	public void setUp() throws Exception {
		client().logout().blockingGet();
	}

	@Test
	public void testNoUserAccess() {
		UserListResponse userListResponse = client().findUsers().blockingGet();
		assertThat(userListResponse.getData()).isEmpty();
	}

	@Test
	public void testNoGraphQlUserAccess() {
		GraphQLResponse graphQLResponse = client().graphqlQuery(PROJECT_NAME, "{\n" +
			"  users {\n" +
			"    elements {\n" +
			"      username\n" +
			"    }\n" +
			"  }\n" +
			"}").blockingGet();
		assertThat(graphQLResponse.getData().getJsonObject("users").getJsonArray("elements").size()).isEqualTo(0);
	}

	@Test
	public void testLogin() {
		tryLogin("nonExistingUser", "wrongPassword", UNAUTHORIZED, "Login failed.");
		tryLogin("admin", "wrongPassword", UNAUTHORIZED, "Login failed.");
	}

	@Test
	public void testCreateUser() {
		UserCreateRequest userCreateRequest = new UserCreateRequest()
			.setUsername("nonExistingUser")
			.setPassword("somePassword");

		call(() -> client().createUser(userCreateRequest), FORBIDDEN);
		userCreateRequest.setUsername("admin");
		call(() -> client().createUser(userCreateRequest), FORBIDDEN);
	}

	@Test
	public void testGraphQl() {
		GraphQLResponse graphQLResponse = client().graphqlQuery(PROJECT_NAME, "{user(name:\"nonExistingUser\") { username }}").blockingGet();
		assertThat(graphQLResponse.getErrors()).isNullOrEmpty();
		assertThat(graphQLResponse.getData().getJsonObject("user")).isNull();

		graphQLResponse = client().graphqlQuery(PROJECT_NAME, "{user(name:\"admin\") { username }}").blockingGet();
		assertThat(graphQLResponse.getErrors()).isNullOrEmpty();
		assertThat(graphQLResponse.getData().getJsonObject("user")).isNull();
	}

	private void tryLogin(String username, String password, HttpResponseStatus expectedStatusCode, String expectedMessage) {
		client().setLogin(username, password);
		try {
			client().login().blockingGet();
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if (cause instanceof MeshRestClientMessageException) {
				assertThat((MeshRestClientMessageException) cause)
					.hasStatusCode(expectedStatusCode.code())
					.hasMessage(expectedMessage);
			} else {
				throw e;
			}
		}
	}
}

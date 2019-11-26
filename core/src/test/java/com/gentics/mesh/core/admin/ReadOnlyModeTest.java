package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;

import com.gentics.mesh.core.rest.admin.runtimeconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.pointer.JsonPointer;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ReadOnlyModeTest extends AbstractMeshTest {

	@After
	public void tearDown() throws Exception {
		setReadOnly(false);
	}

	@Test
	public void testCreateUser() {
		createUser("test1");
		setReadOnly(true);
		call(
			() -> client().createUser(new UserCreateRequest().setUsername("test2").setPassword("abc")),
			HttpResponseStatus.METHOD_NOT_ALLOWED,
			"error_readonly_mode"
		);
		setReadOnly(false);
		createUser("test2");
	}

	@Test
	public void testGraphQL() {
		MeshRequest<GraphQLResponse> query = client().graphqlQuery(PROJECT_NAME, "{ me { uuid } }");
		GraphQLResponse me;

		me = call(() -> query);
		assertNotNull(JsonPointer.from("/me/uuid").queryJson(me.getData()));

		setReadOnly(true);
		me = call(() -> query);
		assertNotNull(JsonPointer.from("/me/uuid").queryJson(me.getData()));

		setReadOnly(false);
		me = call(() -> query);
		assertNotNull(JsonPointer.from("/me/uuid").queryJson(me.getData()));
	}

	private void setReadOnly(boolean readOnly) {
		call(() -> client().updateLocalConfig(new LocalConfigModel().setReadOnly(readOnly)));
	}
}

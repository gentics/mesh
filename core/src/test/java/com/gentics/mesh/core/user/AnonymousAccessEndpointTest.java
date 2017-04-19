package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class AnonymousAccessEndpointTest extends AbstractMeshTest {

	@Test
	public void testAnonymousAccess() {
		client().logout().toCompletable().await();
		UserResponse response = call(() -> client().me());
		assertEquals(MeshAuthHandler.ANONYMOUS_USERNAME, response.getUsername());

		MeshResponse<UserResponse> rawResponse = client().me().invoke();
		latchFor(rawResponse);
		assertThat(rawResponse.getResponse().cookies()).as("Anonymous access should not set any cookie").isEmpty();

		String uuid = db().noTx(() -> content().getUuid());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid);

		try (NoTx noTx = db().noTx()) {
			anonymousRole().grantPermissions(content(), READ_PERM);
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
	}

}

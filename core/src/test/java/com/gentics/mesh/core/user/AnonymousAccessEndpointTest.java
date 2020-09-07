package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class AnonymousAccessEndpointTest extends AbstractMeshTest {

	@Test
	public void testAnonymousAccess() {
		client().logout().ignoreElement().blockingAwait();
		UserResponse response = call(() -> client().me());
		assertEquals(MeshJWTAuthHandler.ANONYMOUS_USERNAME, response.getUsername());

		MeshResponse<UserResponse> rawResponse = client().me().getResponse().blockingGet();
		assertThat(rawResponse.getCookies()).as("Anonymous access should not set any cookie").isEmpty();

		String uuid = db().tx(() -> content().getUuid());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.grantPermissions(anonymousRole(), content(), READ_PERM);
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// Test toggling the anonymous option
		options().getAuthenticationOptions().setEnableAnonymousAccess(false);
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), UNAUTHORIZED, "error_not_authorized");
		options().getAuthenticationOptions().setEnableAnonymousAccess(true);
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// Verify that anonymous access does not work if the anonymous user is deleted
		try (Tx tx = tx()) {
			users().get(MeshJWTAuthHandler.ANONYMOUS_USERNAME).remove();
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), UNAUTHORIZED, "error_not_authorized");
	}

	@Test
	public void testReadPublishedNode() {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.grantPermissions(anonymousRole(), content(), READ_PERM);
			tx.success();
		}

		client().logout().ignoreElement().blockingAwait();
		call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().setVersion("published")));
	}

}

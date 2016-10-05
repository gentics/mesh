package com.gentics.mesh.core.user;

import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.AuthenticationMethod;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;
import rx.Single;

public class AuthenticationVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.authenticationVerticle());
		return list;
	}

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			User user = user();
			String username = user.getUsername();
			String uuid = user.getUuid();

			MeshRestClient client = MeshRestClient.create("localhost", getPort(), Mesh.vertx(), AuthenticationMethod.JWT);
			client.setLogin(username, password());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.toBlocking().value();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			MeshResponse<UserResponse> meResponse = client.me().invoke();
			latchFor(meResponse);
			UserResponse me = meResponse.result();
			assertFalse("The request failed.", meResponse.failed());

			assertNotNull(me);
			assertEquals(uuid, me.getUuid());

			Single<GenericMessageResponse> logoutFuture = client.logout();
			logoutFuture.toBlocking().value();

			// assertTrue(client.getCookie().startsWith(MeshOptions.MESH_SESSION_KEY + "=deleted; Max-Age=0;"));
			meResponse = client.me().invoke();
			latchFor(meResponse);
			expectException(meResponse, UNAUTHORIZED, "error_not_authorized");
		}
	}

}

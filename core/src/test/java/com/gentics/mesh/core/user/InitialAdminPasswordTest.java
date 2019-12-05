package com.gentics.mesh.core.user;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.context.MeshOptionChanger.INITIAL_ADMIN_PASSWORD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT_AND_NODE, startServer = true, optionChanger = INITIAL_ADMIN_PASSWORD)
public class InitialAdminPasswordTest extends AbstractMeshTest {

	@Test
	public void testLogin() {
		client().logout().blockingGet();
		client().setLogin("admin", "debug99");
		call(client().login(), BAD_REQUEST, "auth_login_password_change_required");

		client().setLogin("admin", "debug99", "99debug");
		client().login().blockingGet();
		assertThat(getUser()).doesNotHaveToChangePassword();
	}

	private UserResponse getUser() {
		return client().me().blockingGet();
	}

}

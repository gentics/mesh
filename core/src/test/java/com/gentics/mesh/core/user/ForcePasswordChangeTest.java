package com.gentics.mesh.core.user;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.reactivex.Single;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT_AND_NODE, startServer = true)
public class ForcePasswordChangeTest extends AbstractMeshTest {

	@Test
	public void testForcePasswordChange() {
		assertThat(getUser()).doesNotHaveToChangePassword();
		forcePasswordChange();
		assertThat(getUser()).hasToChangePassword();
		call(client().login(), BAD_REQUEST, "auth_login_password_change_required");
		login("joe1", "test123", "newpw");
		assertThat(getUser()).doesNotHaveToChangePassword();
		login("joe1", "newpw");
	}

	@Test
	public void testForcePasswordChangeWithLogout() {
		assertThat(getUser()).doesNotHaveToChangePassword();
		forcePasswordChange();
		assertThat(getUser()).hasToChangePassword();
		client().logout().blockingGet();
		assertThat(getUser()).isAnonymous();
		call(client().login(), BAD_REQUEST, "auth_login_password_change_required");
		assertThat(getUser()).isAnonymous();
		login("joe1", "test123", "newpw");
		assertThat(getUser())
			.doesNotHaveToChangePassword()
			.hasName("joe1");
		login("joe1", "newpw");
	}

	/**
	 * It should only be possible to change the password on login with the forcePasswordChange flag.
	 * The new password should be ignored if it is sent anyway.
 	 */
	@Test
	public void testPasswordChangeWithoutFlag() {
		assertThat(getUser()).doesNotHaveToChangePassword();
		login("joe1", "test123", "newpw");
		call(loginSingle("joe1", "newpw"), UNAUTHORIZED, "auth_login_failed");
		login("joe1", "test123");
	}

	private void login(String username, String password) {
		loginSingle(username, password).blockingGet();
	}

	private Single<GenericMessageResponse> loginSingle(String username, String password) {
		client().setLogin(username, password);
		return client().login();
	}

	private void login(String username, String password, String newPassword) {
		client().setLogin(username, password, newPassword);
		client().login().blockingGet();
	}

	private void forcePasswordChange() {
		UserResponse user = getUser();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setForcedPasswordChange(true);
		client().updateUser(user.getUuid(), updateRequest).blockingAwait();
	}

	private UserResponse getUser() {
		return client().me().blockingGet();
	}


}

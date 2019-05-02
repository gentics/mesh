package com.gentics.mesh.core.user;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
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

	public static final String USERNAME = "joe1";
	public static final String PASSWORD = "test123";
	public static final String NEW_PASSWORD = "newpw";

	@Test
	public void testForcePasswordChange() {
		assertThat(getUser()).doesNotHaveToChangePassword();
		forcePasswordChange();
		assertThat(getUser()).hasToChangePassword();
		call(client().login(), BAD_REQUEST, "auth_login_password_change_required");
		login(PASSWORD, NEW_PASSWORD);
		assertThat(getUser()).doesNotHaveToChangePassword();
		login(NEW_PASSWORD);
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
		login(PASSWORD, NEW_PASSWORD);
		assertThat(getUser())
			.doesNotHaveToChangePassword()
			.hasName(USERNAME);
		login(NEW_PASSWORD);
	}

	/**
	 * It should only be possible to change the password on login with the forcePasswordChange flag.
	 * The new password should be ignored if it is sent anyway.
 	 */
	@Test
	public void testPasswordChangeWithoutFlag() {
		assertThat(getUser()).doesNotHaveToChangePassword();
		login(PASSWORD, NEW_PASSWORD);
		call(loginSingle(NEW_PASSWORD), UNAUTHORIZED, "auth_login_failed");
		login(PASSWORD);
	}

	@Test
	public void testWithResetToken() {
		forcePasswordChange();
		String resetToken = createResetToken();
		UserResponse user = getUser();
		client().logout();
		assertThat(getUser()).isAnonymous();

		updateUserPassword(user, resetToken);

		assertThat(getUser()).isAnonymous();
		login(NEW_PASSWORD);

		assertThat(getUser())
			.doesNotHaveToChangePassword()
			.hasName(USERNAME);
	}

	private void updateUserPassword(UserResponse user, String resetToken) {
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword(NEW_PASSWORD);
		client().updateUser(user.getUuid(), request, new UserParametersImpl().setToken(resetToken)).blockingAwait();
	}

	private String createResetToken() {
		return client().getUserResetToken(getUser().getUuid()).blockingGet().getToken();
	}

	private void login(String password) {
		loginSingle(password).blockingGet();
	}

	private Single<GenericMessageResponse> loginSingle(String password) {
		client().setLogin(USERNAME, password);
		return client().login();
	}

	private void login(String password, String newPassword) {
		client().setLogin(USERNAME, password, newPassword);
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

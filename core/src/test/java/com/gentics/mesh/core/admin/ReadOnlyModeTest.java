package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;

import java.util.function.IntFunction;

import org.junit.After;
import org.junit.Test;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ReadOnlyModeTest extends AbstractMeshTest {

	@After
	public void tearDown() throws Exception {
		setReadOnly(false);
	}

	@Test
	public void testCreateUser() {
		testReadOnly(false, i -> client().createUser(new UserCreateRequest().setUsername("test" + i).setPassword("abc")));
	}

	@Test
	public void testGraphQL() {
		testReadOnly(true, client().graphqlQuery(PROJECT_NAME, "{ me { uuid } }"));
	}

	/**
	 * Calls the request 3 times: Once without read only mode, once with read only mode, then once again without read only mode.
	 * @param allowedInReadOnly If true, the request is expected to work in read only mode. Otherwise the request is expected to fail in read only mode.
	 */
	private void testReadOnly(boolean allowedInReadOnly, MeshRequest<?> request) {
		testReadOnly(allowedInReadOnly, ignore -> request);
	}

	/**
	 * Calls the request 3 times: Once without read only mode, once with read only mode, then once again without read only mode.
	 * @param allowedInReadOnly If true, the request is expected to work in read only mode. Otherwise the request is expected to fail in read only mode.
	 * @param request A function that returns the request to be tested. The parameter is the call count, starting with 0.
	 */
	private void testReadOnly(boolean allowedInReadOnly, IntFunction<MeshRequest<?>> request) {
		request.apply(0).blockingAwait();
		setReadOnly(true);
		if (allowedInReadOnly) {
			request.apply(1).blockingAwait();
		} else {
			call(() -> request.apply(1), HttpResponseStatus.METHOD_NOT_ALLOWED, "error_readonly_mode");
		}
		setReadOnly(false);
		request.apply(2).blockingAwait();
	}

	private void setReadOnly(boolean readOnly) {
		call(() -> client().updateLocalConfig(new LocalConfigModel().setReadOnly(readOnly)));
	}
}

package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.function.IntFunction;

import org.junit.After;
import org.junit.Test;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.pointer.JsonPointer;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ReadOnlyModeTest extends AbstractMeshTest {

	@After
	public void tearDown() {
		setReadOnly(false);
	}

	@Test
	public void testReadGraphQL() {
		Single<Boolean> request = client().graphqlQuery(PROJECT_NAME, "{ mesh { config { readOnly } } }")
			.toSingle().map(response -> (Boolean)JsonPointer.from("/mesh/config/readOnly").queryJson(response.getData()));

		assertFalse(request.blockingGet());
		setReadOnly(true);
		assertTrue(request.blockingGet());
		setReadOnly(false);
		assertFalse(request.blockingGet());
	}

	@Test
	public void testReadRest() {
		Single<Boolean> request = client().loadLocalConfig()
			.toSingle().map(LocalConfigModel::isReadOnly);

		assertFalse(request.blockingGet());
		setReadOnly(true);
		assertTrue(request.blockingGet());
		setReadOnly(false);
		assertFalse(request.blockingGet());
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

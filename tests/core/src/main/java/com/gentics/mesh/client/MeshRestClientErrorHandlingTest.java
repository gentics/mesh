package com.gentics.mesh.client;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT_AND_NODE, startServer = true)
public class MeshRestClientErrorHandlingTest extends AbstractMeshTest {
	@Before
	public void setUp() throws Exception {
		client().logout();
	}

	@Test
	public void testBlockingGet() {
		expectError(403, () -> {
			forbiddenRequest().blockingGet();
		});
	}

	@Test
	public void testBlockingAwait() {
		expectError(403, () -> {
			forbiddenRequest().blockingAwait();
		});
	}

	@Test
	public void testToCompletable() {
		expectError(403, () -> {
			forbiddenRequest().toCompletable().blockingAwait();
		});
	}

	@Test
	public void testToSingle() {
		expectError(403, () -> {
			forbiddenRequest().toSingle().blockingGet();
		});
	}

	@Test
	public void testToMaybe() {
		expectError(403, () -> {
			forbiddenRequest().toMaybe().blockingGet();
		});
	}

	@Test
	public void testToObservable() {
		expectError(403, () -> {
			forbiddenRequest().toObservable().blockingFirst();
		});
	}

	@Test
	public void testToFlowable() {
		expectError(403, () -> {
			forbiddenRequest().toFlowable().blockingFirst();
		});
	}

	private MeshRequest<RoleResponse> forbiddenRequest() {
		return client().createRole(new RoleCreateRequest().setName("forbiddenRole"));
	}


	private void expectError(int status, Runnable runnable) {
		try {
			runnable.run();
			fail();
		} catch (Throwable err) {
			assertThat(err).hasCauseInstanceOf(MeshRestClientMessageException.class);
			assertThat(((MeshRestClientMessageException) err.getCause()).getStatusCode()).isEqualTo(status);
		}
	}
}

package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true, inMemoryDB = true)
public class ConsistencyCheckTest extends AbstractMeshTest {

	@Test
	public void testConsistencyCheck() {
		client().setLogin("admin", "admin");
		client().login().blockingGet();

		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(ConsistencyRating.CONSISTENT, response.getResult());

		tx(() -> {
			user().getVertex().removeProperty(UserImpl.USERNAME_PROPERTY_KEY);
		});
		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).hasSize(1);
		assertEquals(userUuid(), response.getInconsistencies().get(0).getElementUuid());
		assertEquals(ConsistencyRating.INCONSISTENT, response.getResult());

		// Now fix the inconsistency. Otherwise the asserter of the test (within @After) would fail.
		tx(() -> {
			user().getVertex().setProperty(UserImpl.USERNAME_PROPERTY_KEY, "blub");
		});
	}

}

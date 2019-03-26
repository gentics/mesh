package com.gentics.mesh.core.admin;

import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.CONSISTENT;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.INCONSISTENT;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, inMemoryDB = true)
public class ConsistencyCheckTest extends AbstractMeshTest {

	@Test
	public void testConsistencyCheck() {
		client().setLogin("admin", "admin");
		client().login().blockingGet();

		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(CONSISTENT, response.getResult());

		tx(() -> {
			user().getVertex().removeProperty(UserImpl.USERNAME_PROPERTY_KEY);
		});
		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).hasSize(1);
		assertEquals(userUuid(), response.getInconsistencies().get(0).getElementUuid());
		assertEquals(INCONSISTENT, response.getResult());

		// Now fix the inconsistency. Otherwise the asserter of the test (within @After) would fail.
		tx(() -> {
			user().getVertex().setProperty(UserImpl.USERNAME_PROPERTY_KEY, "blub");
		});
	}

	@Test
	public void testConsistencyRepair() {
		grantAdminRole();

		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(ConsistencyRating.CONSISTENT, response.getResult());

		tx(() -> {
			content().remove();
		});
		response = call(() -> client().checkConsistency());
		assertEquals(INCONSISTENT, response.getResult());
		assertThat(response.getInconsistencies()).hasSize(4);
		InconsistencyInfo info = response.getInconsistencies().get(0);
		assertFalse("The check should not repair the inconsistency", info.isRepaired());
		assertEquals(RepairAction.DELETE, info.getRepairAction());

		expect(REPAIR_START).one();
		expect(REPAIR_FINISHED).one();
		response = call(() -> client().repairConsistency());
		awaitEvents();

		assertEquals(INCONSISTENT, response.getResult());
		// We only see two inconsistencies because the other two were additional versions of the node and were also deleted.
		assertThat(response.getInconsistencies()).hasSize(2);
		info = response.getInconsistencies().get(0);
		assertTrue("The repair should not repair the inconsistency", info.isRepaired());
		assertEquals(RepairAction.DELETE, info.getRepairAction());

		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(ConsistencyRating.CONSISTENT, response.getResult());

	}

}

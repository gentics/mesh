package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractRestEndpointTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

public class RoleVerticlePerformanceTest extends AbstractRestEndpointTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	private void addRoles() {
		for (int i = 0; i < 200; i++) {
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName("Role" + i);
			call(() -> getClient().createRole(request));
		}
	}

	@Test
	public void testPerformance() {
		addRoles();

		String uuid = db.noTx(() -> role().getUuid());

		loggingStopWatch(logger, "role.read-page-100", 200, (step) -> {
			call(() -> getClient().findRoles(new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "role.read-page-25", 200, (step) -> {
			call(() -> getClient().findRoles(new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "role.read-by-uuid", 200, (step) -> {
			call(() -> getClient().findRoleByUuid(uuid));
		});

		loggingStopWatch(logger, "role.create", 200, (step) -> {
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName("NameNew" + step);
			call(() -> getClient().createRole(request));
		});
	}
}

package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

public class UserVerticlePerformanceTest extends AbstractRestVerticleTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	public void addUsers() {
		for (int i = 0; i < 200; i++) {
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("Name" + i);
			request.setPassword("Test1234");
			call(() -> getClient().createUser(request));
		}
	}

	@Test
	public void testPermissionPerformance() {
		loggingStopWatch(logger, "user.hasPermission", 100000, (step) -> {
			try (NoTx noTx = db.noTx()) {
				user().hasPermission(content(), GraphPermission.READ_PERM);
			}
		});
	}

	@Test
	public void testPermissionNamesPerformance() {
		User user = db.noTx(() -> user());
		Node content = db.noTx(() -> content());
		loggingStopWatch(logger, "user.getPermissionNames", 70000, (step) -> {
			try (NoTx noTx = db.noTx()) {
				user.getPermissionNames(content);
			}
		});

	}

	@Test
	public void testPerformance() {
		addUsers();

		String uuid = db.noTx(() -> user().getUuid());

		loggingStopWatch(logger, "user.read-page-100", 200, (step) -> {
			call(() -> getClient().findUsers(new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "user.read-page-25", 200, (step) -> {
			call(() -> getClient().findUsers(new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "user.read-by-uuid", 200, (step) -> {
			call(() -> getClient().findUserByUuid(uuid));
		});

		loggingStopWatch(logger, "user.create", 200, (step) -> {
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("NameNew" + step);
			request.setPassword("Test1234");
			call(() -> getClient().createUser(request));
		});
	}
}

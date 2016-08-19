package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

public class UserVerticlePerformanceTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private UserVerticle verticle;

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

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
		loggingStopWatch(logger, "user.hasPermission", 70000, (step) -> {
			try (NoTx noTx = db.noTx()) {
				user().hasPermission(content(), GraphPermission.READ_PERM);
			}
		});
	}

	@Test
	public void testPermissionNamesAsyncPerformance() {
		loggingStopWatch(logger, "user.getPermissionNamesAsync", 70000, (step) -> {
			InternalActionContext ac = Mocks.getMockedInternalActionContext();
			try (NoTx noTx = db.noTx()) {
				user().getPermissionNamesAsync(ac, content()).doOnSuccess(list -> {

				}).toBlocking().value();
			}
		});
	}

	@Test
	public void testPermissionNamesPerformance() {
		loggingStopWatch(logger, "user.getPermissionNames", 70000, (step) -> {
			InternalActionContext ac = Mocks.getMockedInternalActionContext();
			try (NoTx noTx = db.noTx()) {
				user().getPermissionNames(ac, content());
			}
		});
	}

	@Test
	public void testPerformance() {
		addUsers();

		loggingStopWatch(logger, "user.read-page-100", 200, (step) -> {
			call(() -> getClient().findUsers(new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "user.read-page-25", 200, (step) -> {
			call(() -> getClient().findUsers(new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "user.create", 200, (step) -> {
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("NameNew" + step);
			request.setPassword("Test1234");
			call(() -> getClient().createUser(request));
		});
	}
}

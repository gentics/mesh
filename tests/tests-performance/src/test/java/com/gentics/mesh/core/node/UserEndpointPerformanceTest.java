package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.performance.StopWatchLogger;
@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true)
public class UserEndpointPerformanceTest extends AbstractMeshTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	public void addUsers() {
		for (int i = 0; i < 200; i++) {
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("Name" + i);
			request.setPassword("Test1234");
			call(() -> client().createUser(request));
		}
	}

	@Test
	public void testPermissionPerformance() {
		loggingStopWatch(logger, "user.hasPermission", 100000, (step) -> {
			try (Tx tx = tx()) {
				UserDaoWrapper userDao = tx.userDao();
				userDao.hasPermission(user(), content(), InternalPermission.READ_PERM);
			}
		});
	}

	@Test
	public void testPermissionInfoPerformance() {
		HibUser user = tx(() -> user());
		HibNode content = tx(() -> content());
		loggingStopWatch(logger, "user.getPermissionInfo", 70000, (step) -> {
			try (Tx tx = tx()) {
				UserDaoWrapper userDao = tx.userDao();
				userDao.getPermissionInfo(user, content);
			}
		});

	}

	@Test
	public void testPerformance() {
		addUsers();

		String uuid = tx(() -> user().getUuid());

		loggingStopWatch(logger, "user.read-page-100", 200, (step) -> {
			call(() -> client().findUsers(new PagingParametersImpl().setPerPage(100L)));
		});

		loggingStopWatch(logger, "user.read-page-25", 200, (step) -> {
			call(() -> client().findUsers(new PagingParametersImpl().setPerPage(25L)));
		});

		loggingStopWatch(logger, "user.read-by-uuid", 200, (step) -> {
			call(() -> client().findUserByUuid(uuid));
		});

		loggingStopWatch(logger, "user.create", 200, (step) -> {
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("NameNew" + step);
			request.setPassword("Test1234");
			call(() -> client().createUser(request));
		});
	}
}

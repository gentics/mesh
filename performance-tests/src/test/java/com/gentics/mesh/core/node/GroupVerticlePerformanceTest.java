package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

import io.vertx.core.AbstractVerticle;

public class GroupVerticlePerformanceTest extends AbstractIsolatedRestVerticleTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.groupVerticle());
		return list;
	}

	private void addGroups() {
		for (int i = 0; i < 200; i++) {
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName("Group" + i);
			call(() -> getClient().createGroup(request));
		}
	}

	@Test
	public void testPerformance() {
		addGroups();

		String uuid = db.noTx(() -> group().getUuid());

		loggingStopWatch(logger, "group.read-page-100", 200, (step) -> {
			call(() -> getClient().findGroups(new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "group.read-page-25", 200, (step) -> {
			call(() -> getClient().findGroups(new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "group.read-by-uuid", 200, (step) -> {
			call(() -> getClient().findGroupByUuid(uuid));
		});

		loggingStopWatch(logger, "group.create", 200, (step) -> {
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName("NameNew" + step);
			call(() -> getClient().createGroup(request));
		});
	}

}

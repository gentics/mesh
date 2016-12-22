package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.AbstractRestEndpointTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

public class ProjectEndpointPerformanceTest extends AbstractRestEndpointTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	private void addProjects() {
		for (int i = 0; i < 200; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("NewProject" + i);
			request.setSchema(new SchemaReference().setName("content"));
			call(() -> getClient().createProject(request));
		}
	}

	@Test
	public void testPerformance() {
		addProjects();

		String uuid = db.noTx(() -> project().getUuid());

		loggingStopWatch(logger, "project.read-page-100", 200, (step) -> {
			call(() -> getClient().findProjects(new PagingParametersImpl().setPerPage(100)));
		});

		loggingStopWatch(logger, "project.read-page-25", 200, (step) -> {
			call(() -> getClient().findProjects(new PagingParametersImpl().setPerPage(25)));
		});

		loggingStopWatch(logger, "project.read-by-uuid", 200, (step) -> {
			call(() -> getClient().findProjectByUuid(uuid));
		});

		loggingStopWatch(logger, "project.create", 200, (step) -> {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("Project" + step);
			request.setSchema(new SchemaReference().setName("content"));
			call(() -> getClient().createProject(request));
		});
	}

}

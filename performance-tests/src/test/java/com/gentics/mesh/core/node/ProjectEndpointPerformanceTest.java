package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ProjectEndpointPerformanceTest extends AbstractMeshTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	private void addProjects() {
		for (int i = 0; i < 200; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("NewProject" + i);
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			call(() -> client().createProject(request));
		}
	}

	@Test
	public void testPerformance() {
		addProjects();

		String uuid = db().tx(() -> project().getUuid());

		loggingStopWatch(logger, "project.read-page-100", 200, (step) -> {
			call(() -> client().findProjects(new PagingParametersImpl().setPerPage(100L)));
		});
		
		loggingStopWatch(logger, "project.read-page-25", 200, (step) -> {
			call(() -> client().findProjects(new PagingParametersImpl().setPerPage(25L)));
		});

		loggingStopWatch(logger, "project.read-by-uuid", 200, (step) -> {
			call(() -> client().findProjectByUuid(uuid));
		});

		loggingStopWatch(logger, "project.create", 200, (step) -> {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("Project" + step);
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			call(() -> client().createProject(request));
		});
	}

}

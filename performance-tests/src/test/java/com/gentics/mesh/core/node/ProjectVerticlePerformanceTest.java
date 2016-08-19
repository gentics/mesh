package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

public class ProjectVerticlePerformanceTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private ProjectVerticle verticle;

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	private void addProjects() {
		for (int i = 0; i < 200; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("NewProject" + i);
			request.setSchemaReference(new SchemaReference().setName("content"));
			call(() -> getClient().createProject(request));
		}
	}

	@Test
	public void testPerformance() {
		addProjects();

		String uuid = db.noTx(() -> project().getUuid());

		loggingStopWatch(logger, "project.read-page-100", 200, (step) -> {
			call(() -> getClient().findProjects(new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "project.read-page-25", 200, (step) -> {
			call(() -> getClient().findProjects(new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "project.read-by-uuid", 200, (step) -> {
			call(() -> getClient().findProjectByUuid(uuid));
		});

		loggingStopWatch(logger, "project.create", 200, (step) -> {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("Project" + step);
			request.setSchemaReference(new SchemaReference().setName("content"));
			call(() -> getClient().createProject(request));
		});
	}

}

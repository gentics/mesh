package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_TOXIC;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import eu.rekawek.toxiproxy.model.ToxicDirection;

@MeshTestSetting(elasticsearch = CONTAINER_TOXIC, startServer = true, testSize = FULL)
public class ElasticsearchResilienceTest extends AbstractMeshTest {

	@Test
	public void testES() throws IOException {
		toxics().latency("latency", ToxicDirection.UPSTREAM, 500);

		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		String newProjectName = "updatedprojectname";
		updateProject(project.getUuid(), newProjectName);

		waitForSearchIdleEvent();
		ProjectListResponse response = client().searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl()
			.setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(0, response.getData().size());

	}

}

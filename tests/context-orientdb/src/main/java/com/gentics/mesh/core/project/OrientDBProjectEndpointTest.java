package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.isFailureMessage;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class OrientDBProjectEndpointTest extends ProjectEndpointTest {


	/**
	 * Test renaming, deleting and re-creating a project (together with project name cache).
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	@Test
	public void testRenameDeleteCreateProject() throws InterruptedException, TimeoutException {
		// create project named "project"
		ProjectResponse project = createProject("project");

		// get tag families of project (this will put project into cache)
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		// rename project to "newproject"
		project = updateProject(project.getUuid(), "newproject");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		long maxWaitMs = 1000;
		long delayMs = 100;
		boolean repeat = false;

		// get tag families of newproject (this will put project into cache)
		long start = System.currentTimeMillis();
		do {
			try {
				call(() -> client().findTagFamilies("newproject"));
				repeat = false;
			} catch (Throwable t) {
				if (isFailureMessage(t, HttpResponseStatus.NOT_FOUND) && (System.currentTimeMillis() - start) < maxWaitMs) {
					Thread.sleep(delayMs);
					repeat = true;
				} else {
					throw t;
				}
			}
		} while (repeat);
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		// delete "newproject"
		deleteProject(project.getUuid());
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// create (again)
		project = createProject("project");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// get tag families of project
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);
	}

}

package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import com.gentics.mesh.test.util.MeshAssert;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = FULL)
public class ProjectSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public ProjectSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSearchProject() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		waitForSearchIdleEvent();

		ProjectListResponse response = client().searchProjects(getSimpleQuery("name", "dummy"), new PagingParametersImpl().setPage(1)
			.setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());

		response = client().searchProjects(getSimpleQuery("name", "blub"), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(0, response.getData().size());

		response = client().searchProjects(getSimpleTermQuery("name.raw", "dummy"), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {

		final String newName = "newproject";
		ProjectResponse project = createProject(newName);
		try (Tx tx = tx()) {
			MeshAssert.assertElement(boot().projectRoot(), project.getUuid(), true);
		}
		waitForSearchIdleEvent();
		ProjectListResponse response = call(() -> client().searchProjects(getSimpleTermQuery("name.raw", newName), new PagingParametersImpl().setPage(
			1).setPerPage(2L)));
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		waitForSearchIdleEvent();
		ProjectListResponse response = client().searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl()
			.setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());

		deleteProject(project.getUuid());
		waitForSearchIdleEvent();
		response = client().searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals(0, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		String newProjectName = "updatedprojectname";
		updateProject(project.getUuid(), newProjectName);

		waitForSearchIdleEvent();
		ProjectListResponse response = client().searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl()
			.setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(0, response.getData().size());

		waitForSearchIdleEvent();

		response = client().searchProjects(getSimpleTermQuery("name.raw", newProjectName), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals(1, response.getData().size());
	}

}

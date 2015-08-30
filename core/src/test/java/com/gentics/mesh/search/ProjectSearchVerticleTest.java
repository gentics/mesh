package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Future;

public class ProjectSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(projectVerticle);
		return list;
	}

	@Test
	public void testSearchProject() throws InterruptedException, JSONException {
		try (Trx tx = db.trx()) {
			boot.meshRoot().getSearchQueue().addFullIndex();
			tx.success();
		}

		Future<ProjectListResponse> future = getClient().searchProjects(getSimpleQuery("dummy"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse response = future.result();
		assertEquals(1, response.getData().size());

		future = getClient().searchProjects(getSimpleQuery("blub"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(0, response.getData().size());

		future = getClient().searchProjects(getSimpleTermQuery("name", "dummy"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	public void testSearchCreatedProject() throws InterruptedException {
		final String newName = "newProject";
		ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
		projectCreateRequest.setName(newName);
		Future<ProjectResponse> createFuture = getClient().createProject(projectCreateRequest);
		latchFor(createFuture);
		assertSuccess(createFuture);

		Future<ProjectListResponse> future = getClient().searchProjects(getSimpleTermQuery("name", newName),
				new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

}

package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;

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
		setupFullIndex();

		QueryBuilder qb = QueryBuilders.queryStringQuery("dummy");
		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(qb.toString()));
		Future<ProjectListResponse> future = getClient().searchProjects(request.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

}

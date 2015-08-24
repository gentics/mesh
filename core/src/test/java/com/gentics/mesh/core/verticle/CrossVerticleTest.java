package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectTagFamilyVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class CrossVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Autowired
	private ProjectTagFamilyVerticle tagFamilyVerticle;

	@Autowired
	private ProjectNodeVerticle nodeVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(projectVerticle);
		list.add(tagFamilyVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testAccessNewProjectRoute() {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		Future<ProjectResponse> future = getClient().createProject(request);
		latchFor(future);
		assertSuccess(future);

		Future<NodeListResponse> nodelistFuture = getClient().findNodes(name);
		latchFor(nodelistFuture);
		assertSuccess(nodelistFuture);

		Future<TagFamilyListResponse> tagFamilyListFuture = getClient().findTagFamilies(name);
		latchFor(tagFamilyListFuture);
		assertSuccess(tagFamilyListFuture);

	}

}

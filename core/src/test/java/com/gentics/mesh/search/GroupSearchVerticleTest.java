package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;

import io.vertx.core.Future;

public class GroupSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private GroupVerticle groupVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(groupVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(groupName);

		Future<GroupResponse> future = getClient().createGroup(request);
		latchFor(future);
		assertSuccess(future);

		Future<GroupListResponse> searchFuture = getClient().searchGroups(getSimpleTermQuery("name", groupName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

}

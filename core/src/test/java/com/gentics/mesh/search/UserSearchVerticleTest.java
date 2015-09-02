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
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.user.UserVerticle;

import io.vertx.core.Future;

public class UserSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(userVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {

		String username = "testuser42a";
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group().getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", username));
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

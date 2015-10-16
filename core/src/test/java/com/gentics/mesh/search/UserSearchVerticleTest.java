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
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;

import io.vertx.core.Future;

public class UserSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private UserVerticle userVerticle;
	
	@Autowired
	private GroupVerticle groupVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(userVerticle);
		list.add(groupVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {

		String username = "testuser42a";
		createUser(username);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", username));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	public void testSearchUserForGroup() throws InterruptedException, JSONException {

		String groupName = group().getName();
		String username = "extrauser42a";
		createUser(username);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}
	
	@Test
	public void testSearchForAddedUser() throws InterruptedException, JSONException {
		GroupResponse group = createGroup("apa-otsAdmin");
		String groupName = group.getName();
		String username = "extrauser42a";
		
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group.getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}
	
	@Test
	public void testSearchForLaterAddedUser() throws InterruptedException, JSONException {
		GroupResponse group = createGroup("apa-otsAdmin");
		String groupName = group.getName();
		String username = "extrauser42a";
		
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);
		
		Future<GroupResponse> futureAdd = getClient().addUserToGroup(group.getUuid(), future.result().getUuid());
		latchFor(futureAdd);
		assertSuccess(futureAdd);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}
	
	@Test
	public void testSearchForRemovedUser() throws InterruptedException, JSONException {
		GroupResponse group = createGroup("apa-otsAdmin");
		String groupName = group.getName();
		String username = "extrauser42a";
		
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group.getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);
		
		String userUuid = future.result().getUuid();
		
		Future<GroupResponse> futureDelete = getClient().removeUserFromGroup(group.getUuid(), userUuid);
		latchFor(futureDelete);
		assertSuccess(futureDelete);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

	}
	
	@Test
	public void testSearchForDeletedUser() throws InterruptedException, JSONException {
		GroupResponse group = createGroup("apa-otsAdmin");
		String groupName = group.getName();
		String username = "extrauser42a";
		
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group.getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);
		
		String userUuid = future.result().getUuid();
		
		Future<GenericMessageResponse> futureDelete = getClient().deleteUser(userUuid);
		latchFor(futureDelete);
		assertSuccess(futureDelete);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

	}

	@Test
	public void testSearchUserWithPerPageZero() throws InterruptedException, JSONException {

		String groupName = group().getName();
		String username = "extrauser42a";
		createUser(username);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()),
				new PagingInfo().setPerPage(0));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
		assertEquals(1, searchFuture.result().getMetainfo().getTotalCount());

	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		UserResponse user = createUser(userName);
		deleteUser(user.getUuid());

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		UserResponse user = createUser(userName);

		String newUserName = "testgrouprenamed";
		user = updateUser(user.getUuid(), newUserName);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

		searchFuture = getClient().searchUsers(getSimpleTermQuery("username", newUserName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

}

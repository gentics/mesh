package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.query.impl.PagingParameter;

import io.vertx.core.Future;

public class UserSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private GroupVerticle groupVerticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
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
	public void testTokenzierIssueQuery() throws InterruptedException, JSONException {

		String impossibleName = "Jöhä@sRe2";
		user().setLastname(impossibleName);
		fullIndex();
		Future<UserListResponse> future = getClient().searchUsers(getSimpleTermQuery("lastname", impossibleName));
		latchFor(future);
		assertSuccess(future);
		UserListResponse response = future.result();
		assertNotNull(response);
		assertFalse("The user with the name {" + impossibleName + "} could not be found using a simple term query.", response.getData().isEmpty());
		assertEquals(1, response.getData().size());
		assertEquals(impossibleName, response.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueQuery2() throws InterruptedException, JSONException {
		String impossibleName = "Jöhä@sRe";
		user().setLastname(impossibleName);
		fullIndex();
		Future<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName + "*"));
		latchFor(future);
		assertSuccess(future);
		UserListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());
		assertEquals(1, response.getData().size());
		assertEquals(impossibleName, response.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueLowercasedQuery() throws InterruptedException, JSONException {
		String impossibleName = "Jöhä@sRe";
		user().setLastname(impossibleName);
		fullIndex();
		Future<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName.toLowerCase() + "*"));
		latchFor(future);
		assertSuccess(future);
		UserListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());
		assertEquals(1, response.getData().size());
		assertEquals(impossibleName, response.getData().get(0).getLastname());
	}

	@Test
	public void testSearchForUserByEmail() throws InterruptedException, JSONException {
		String email = "testmail@test.com";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername("testuser42a");
		request.setPassword("test1234");
		request.setEmailAddress(email);
		request.setGroupUuid(group().getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleWildCardQuery("email", "*"));
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
		GroupResponse group = createGroup("apa");
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
		// 1. Create a group
		GroupResponse group = createGroup("apaotsadmin");
		String groupName = group.getName();
		String username = "extrauser42a";

		// 2. Create a new user
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);

		// 3. Assign the previously created user to the group 
		Future<GroupResponse> futureAdd = getClient().addUserToGroup(group.getUuid(), future.result().getUuid());
		latchFor(futureAdd);
		assertSuccess(futureAdd);

		// Check whether the user index was updated
		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(
				"We assigned the user to the group and thus the index should have been update but we were unable to find the user with the specified group.",
				1, searchFuture.result().getData().size());

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
				new PagingParameter().setPerPage(0));
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

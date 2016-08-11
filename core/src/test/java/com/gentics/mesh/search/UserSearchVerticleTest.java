package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParameters;

import io.vertx.core.Future;

public class UserSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private GroupVerticle groupVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(userVerticle);
		list.add(groupVerticle);
		return list;
	}

	@Test
	public void testSimpleQuerySearch() {

		String username = "testuser42a";
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		String json = "{\n" + "  \"query\": {\n" + "      \"simple_query_string\" : {\n" + "          \"query\": \"testuser*\",\n"
				+ "          \"analyzer\": \"snowball\",\n" + "          \"fields\": [\"name^5\",\"_all\"],\n"
				+ "          \"default_operator\": \"and\"\n" + "      }\n" + "  }\n" + "}";

		Future<UserListResponse> searchFuture = getClient().searchUsers(json).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
		assertEquals("The found element is not the user we were looking for", username, searchFuture.result().getData().get(0).getUsername());

	}

	@Test
	public void testEmptyResult() {
		String username = "testuser42a";
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		String json = "{\n" + "  \"query\": {\n" + "      \"simple_query_string\" : {\n" + "          \"query\": \"testuser111235*\",\n"
				+ "          \"analyzer\": \"snowball\",\n" + "          \"fields\": [\"name^5\",\"_all\"],\n"
				+ "          \"default_operator\": \"and\"\n" + "      }\n" + "  }\n" + "}";

		Future<UserListResponse> searchFuture = getClient().searchUsers(json).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {

		String username = "testuser42a";
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", username)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	public void testTokenzierIssueQuery() throws Exception {

		String impossibleName = "Jöhä@sRe2";
		try (NoTx noTx = db.noTx()) {
			user().setLastname(impossibleName);
			fullIndex();
		}
		Future<UserListResponse> future = getClient().searchUsers(getSimpleTermQuery("lastname", impossibleName)).invoke();
		latchFor(future);
		assertSuccess(future);
		UserListResponse response = future.result();
		assertNotNull(response);
		assertFalse("The user with the name {" + impossibleName + "} could not be found using a simple term query.", response.getData().isEmpty());
		assertEquals(1, response.getData().size());
		assertEquals(impossibleName, response.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueQuery2() throws Exception {
		String impossibleName = "Jöhä@sRe";
		try (NoTx noTx = db.noTx()) {
			user().setLastname(impossibleName);
			fullIndex();
		}
		Future<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName + "*")).invoke();
		latchFor(future);
		assertSuccess(future);
		ListResponse<UserResponse> response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());
		assertEquals(1, response.getData().size());
		assertEquals(impossibleName, response.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueLowercasedQuery() throws Exception {
		String impossibleName = "Jöhä@sRe";
		try (NoTx noTx = db.noTx()) {
			user().setLastname(impossibleName);
			fullIndex();
		}
		Future<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName.toLowerCase() + "*")).invoke();
		latchFor(future);
		assertSuccess(future);
		ListResponse<UserResponse> response = future.result();
		assertNotNull(response);
		assertTrue(
				"No user should be found since the lastname field is not tokenized anymore thus it is not possible to search with a lowercased term.",
				response.getData().isEmpty());
	}

	@Test
	public void testSearchForUserByEmail() throws InterruptedException, JSONException {
		String email = "testmail@test.com";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername("testuser42a");
		request.setPassword("test1234");
		request.setEmailAddress(email);
		request.setGroupUuid(db.noTx(() -> group().getUuid()));

		Future<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleWildCardQuery("emailaddress", "*")).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals("We expected to see one result.", 1, searchFuture.result().getData().size());
	}

	@Test
	public void testSearchUserForGroup() throws InterruptedException, JSONException {

		String username = "extrauser42a";
		String groupName = db.noTx(() -> group().getName());
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals("We expected to see one result.", 1, searchFuture.result().getData().size());

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

		Future<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals("We expected to see one result.", 1, searchFuture.result().getData().size());

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
		Future<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		// 3. Assign the previously created user to the group
		Future<GroupResponse> futureAdd = getClient().addUserToGroup(group.getUuid(), future.result().getUuid()).invoke();
		latchFor(futureAdd);
		assertSuccess(futureAdd);

		// Check whether the user index was updated
		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		Future<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		String userUuid = future.result().getUuid();

		Future<GroupResponse> futureDelete = getClient().removeUserFromGroup(group.getUuid(), userUuid).invoke();
		latchFor(futureDelete);
		assertSuccess(futureDelete);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		Future<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		String userUuid = future.result().getUuid();

		Future<GenericMessageResponse> futureDelete = getClient().deleteUser(userUuid).invoke();
		latchFor(futureDelete);
		assertSuccess(futureDelete);

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

	}

	@Test
	public void testSearchUserWithPerPageZero() throws InterruptedException, JSONException {

		String groupName = db.noTx(() -> group().getName());
		String username = "extrauser42a";
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()),
				new PagingParameters().setPerPage(0)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
		assertEquals(1, searchFuture.result().getMetainfo().getTotalCount());

	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		try (NoTx noTx = db.noTx()) {
			UserResponse user = createUser(userName);
			deleteUser(user.getUuid());
		}

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		String newUserName = "testgrouprenamed";
		try (NoTx noTx = db.noTx()) {
			UserResponse user = createUser(userName);
			user = updateUser(user.getUuid(), newUserName);
		}

		Future<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

		searchFuture = getClient().searchUsers(getSimpleTermQuery("username", newUserName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

}

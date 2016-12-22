package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;

public class UserSearchEndpointTest extends AbstractSearchEndpointTest implements BasicSearchCrudTestcases {

	@Test
	public void testSimpleQuerySearch() {

		String username = "testuser42a";
		try (NoTx noTx = db.noTx()) {
			createUser(username);
		}

		String json = "{\n" + "  \"query\": {\n" + "      \"simple_query_string\" : {\n" + "          \"query\": \"testuser*\",\n"
				+ "          \"analyzer\": \"snowball\",\n" + "          \"fields\": [\"name^5\",\"_all\"],\n"
				+ "          \"default_operator\": \"and\"\n" + "      }\n" + "  }\n" + "}";

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(json).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(json).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", username)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	public void testTokenzierIssueQuery() throws Exception {

		String impossibleName = "Jöhä@sRe2";
		try (NoTx noTx = db.noTx()) {
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> getClient().updateUser(user().getUuid(), updateRequest));
		}
		MeshResponse<UserListResponse> future = getClient().searchUsers(getSimpleTermQuery("lastname", impossibleName)).invoke();
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
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> getClient().updateUser(user().getUuid(), updateRequest));
		}
		MeshResponse<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName + "*")).invoke();
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
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> getClient().updateUser(user().getUuid(), updateRequest));
		}
		MeshResponse<UserListResponse> future = getClient().searchUsers(getSimpleWildCardQuery("lastname", "*" + impossibleName.toLowerCase() + "*"))
				.invoke();
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

		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleWildCardQuery("emailaddress", "*")).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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
		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		// 3. Assign the previously created user to the group
		MeshResponse<GroupResponse> futureAdd = getClient().addUserToGroup(group.getUuid(), future.result().getUuid()).invoke();
		latchFor(futureAdd);
		assertSuccess(futureAdd);

		// Check whether the user index was updated
		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		String userUuid = future.result().getUuid();

		call(() -> getClient().removeUserFromGroup(group.getUuid(), userUuid));

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);

		String userUuid = future.result().getUuid();

		MeshResponse<Void> futureDelete = getClient().deleteUser(userUuid).invoke();
		latchFor(futureDelete);
		assertSuccess(futureDelete);

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase())).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient()
				.searchUsers(getSimpleTermQuery("groups.name", groupName.toLowerCase()), new PagingParametersImpl().setPerPage(0)).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName)).invoke();
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

		MeshResponse<UserListResponse> searchFuture = getClient().searchUsers(getSimpleTermQuery("username", userName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

		searchFuture = getClient().searchUsers(getSimpleTermQuery("username", newUserName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

}

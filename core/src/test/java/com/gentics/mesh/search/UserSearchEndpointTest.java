package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleWildCardQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class UserSearchEndpointTest extends AbstractMeshTest implements BasicSearchCrudTestcases {

	@Test
	public void testSimpleQuerySearch() throws IOException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(1, list.getData().size());
		assertEquals("The found element is not the user we were looking for", username, list.getData().get(0).getUsername());

	}

	@Test
	public void testPaging() throws IOException {
		String username = "testuser";
		try (Tx tx = tx()) {
			for (int i = 0; i < 100; i++) {
				createUser(username + i);
			}
		}

		testContext.waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json, new PagingParametersImpl(2, 25L)));
		assertEquals("The page should be full.", 25, list.getData().size());
		assertEquals("The page did not match.", 2, list.getMetainfo().getCurrentPage());
		assertEquals("The page count did not match.", 4, list.getMetainfo().getPageCount());
		assertEquals("The total count did not match.", 100, list.getMetainfo().getTotalCount());

	}

	@Test
	public void testBogusQuery() throws IOException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		String json = "someBogusInput";
		call(() -> client().searchUsers(json), BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testEmptyResult() throws IOException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		String json = getESText("userBogusName.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", username)));
		assertEquals(1, list.getData().size());
	}

	@Test
	public void testTokenzierIssueQuery() throws Exception {
		String impossibleName = "Jöhä@sRe2";
		try (Tx tx = tx()) {
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> client().updateUser(user().getUuid(), updateRequest));
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("lastname.raw", impossibleName)));
		assertNotNull(list);
		assertFalse("The user with the name {" + impossibleName + "} could not be found using a simple term query.", list.getData().isEmpty());
		assertEquals(1, list.getData().size());
		assertEquals(impossibleName, list.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueQuery2() throws Exception {
		String impossibleName = "Jöhä@sRe";
		try (Tx tx = tx()) {
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> client().updateUser(user().getUuid(), updateRequest));
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("lastname.raw", "*" + impossibleName + "*")));
		assertNotNull(list);
		assertFalse(list.getData().isEmpty());
		assertEquals(1, list.getData().size());
		assertEquals(impossibleName, list.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueLowercasedQuery() throws Exception {
		String impossibleName = "Jöhä@sRe";
		try (Tx tx = tx()) {
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setLastname(impossibleName);
			call(() -> client().updateUser(user().getUuid(), updateRequest));
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("lastname.raw", "*" + impossibleName.toLowerCase() + "*")));

		assertNotNull(list);
		assertTrue(
			"No user should be found since the lastname field is not tokenized anymore thus it is not possible to search with a lowercased term.",
			list.getData().isEmpty());
	}

	@Test
	public void testSearchForUserByEmail() throws InterruptedException, JSONException {
		String email = "testmail@test.com";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername("testuser42a");
		request.setPassword("test1234");
		request.setEmailAddress(email);
		request.setGroupUuid(db().tx(() -> group().getUuid()));

		call(() -> client().createUser(request));

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("emailaddress", "*")));
		assertEquals("We expected to see two results.", 2, list.getData().size());
	}

	@Test
	public void testSearchUserForGroup() throws InterruptedException, JSONException {

		String username = "extrauser42a";
		String groupName = db().tx(() -> group().getName());
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())));
		assertEquals("We expected to see two results.", 2, list.getData().size());
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

		call(() -> client().createUser(request));

		testContext.waitForSearchIdleEvent();

		UserListResponse searchResponse = client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())).blockingGet();
		assertEquals("We expected to see one result.", 1, searchResponse.getData().size());
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
		UserResponse user = call(() -> client().createUser(request));


		// 3. Assign the previously created user to the group
		call(() -> client().addUserToGroup(group.getUuid(), user.getUuid()));


		testContext.waitForSearchIdleEvent();

		// Check whether the user index was updated
		UserListResponse response = call(() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())));
		System.out.println(response.toJson());
		assertEquals(
			"We assigned the user to the group and thus the index should have been updated but we were unable to find the user with the specified group.",
			1, response.getData().size());
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

		UserResponse response = client().createUser(request).blockingGet();

		String userUuid = response.getUuid();

		call(() -> client().removeUserFromGroup(group.getUuid(), userUuid));

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())));
		assertEquals(0, list.getData().size());

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

		UserResponse user = call(() -> client().createUser(request));

		String userUuid = user.getUuid();
		call(() -> client().deleteUser(userUuid));

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())));
		assertEquals(0, list.getData().size());
	}

	@Test
	public void testSearchUserWithPerPageZero() throws InterruptedException, JSONException {

		String groupName = db().tx(() -> group().getName());
		String username = "extrauser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(
			() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase()), new PagingParametersImpl().setPerPage(0L)));
		assertEquals(0, list.getData().size());
		assertEquals(2, list.getMetainfo().getTotalCount());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		try (Tx tx = tx()) {
			UserResponse user = createUser(userName);
			call(() -> client().deleteUser(user.getUuid()));
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username", userName)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		String newUserName = "testgrouprenamed";
		try (Tx tx = tx()) {
			UserResponse user = createUser(userName);
			user = updateUser(user.getUuid(), newUserName);
		}

		testContext.waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", userName)));
		assertEquals(0, list.getData().size());

		list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", newUserName)));
		assertEquals(1, list.getData().size());
	}

}

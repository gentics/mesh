package com.gentics.mesh.search;

import static com.gentics.mesh.search.index.AbstractSearchHandler.DEFAULT_SEARCH_PER_PAGE;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class UserSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public UserSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSimpleQuerySearch() throws IOException {
		String username = "testuser42a";
		createUser(username);

		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(1, list.getData().size());
		assertEquals("The found element is not the user we were looking for", username, list.getData().get(0).getUsername());
	}

	@Test
	public void testSearchWithUTF8() throws IOException {
		String TEST_CN = "\u6D4B\u8BD5"; // test
		String LONG_DASH = "\u2013";
		String name = "testuser_" + TEST_CN + "_" + LONG_DASH + "_" + TEST_CN;
		createUser(name);

		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals("The page should be full.", 1, list.getData().size());
		assertEquals("The page did not match.", 1, list.getMetainfo().getCurrentPage());
		assertEquals("The page count did not match.", 1, list.getMetainfo().getPageCount());
		assertEquals("The total count did not match.", 1, list.getMetainfo().getTotalCount());
	}

	@Test
	public void testPaging() throws IOException {
		String username = "testuser";
		for (int i = 0; i < 100; i++) {
			createUser(username + i);
		}

		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json, new PagingParametersImpl(2, 25L)));
		assertEquals("The page should be full.", 25, list.getData().size());
		assertEquals("The page did not match.", 2, list.getMetainfo().getCurrentPage());
		assertEquals("The page count did not match.", 4, list.getMetainfo().getPageCount());
		assertEquals("The total count did not match.", 100, list.getMetainfo().getTotalCount());
	}

	@Test
	public void testPagingWithoutPagingParameters() throws IOException {
		String username = "testuser";
		for (int i = 0; i < 20; i++) {
			createUser(username + i);
		}

		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals("The page should be full.", DEFAULT_SEARCH_PER_PAGE, list.getData().size());
		assertEquals("The page did not match.", 1, list.getMetainfo().getCurrentPage());
		assertEquals("The page count did not match.", 2, list.getMetainfo().getPageCount());
		assertEquals("The total count did not match.", 20, list.getMetainfo().getTotalCount());
	}

	@Test
	public void testBogusQuery() throws IOException {
		String username = "testuser42a";
		createUser(username);

		waitForSearchIdleEvent();

		String json = "someBogusInput";
		call(() -> client().searchUsers(json), BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testEmptyResult() throws IOException {
		String username = "testuser42a";
		createUser(username);

		waitForSearchIdleEvent();

		String json = getESText("userBogusName.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String username = "testuser42a";
		createUser(username);

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", username)));
		assertEquals(1, list.getData().size());
	}

	@Test
	public void testTokenzierIssueQuery() throws Exception {
		String impossibleName = "Jöhä@sRe2";
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setLastname(impossibleName);
		call(() -> client().updateUser(userUuid(), updateRequest));

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("lastname.raw", impossibleName)));
		assertNotNull(list);
		assertFalse("The user with the name {" + impossibleName + "} could not be found using a simple term query.", list.getData().isEmpty());
		assertEquals(1, list.getData().size());
		assertEquals(impossibleName, list.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueQuery2() throws Exception {
		String impossibleName = "Jöhä@sRe";
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setLastname(impossibleName);
		call(() -> client().updateUser(userUuid(), updateRequest));

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("lastname.raw", "*" + impossibleName + "*")));
		assertNotNull(list);
		assertFalse(list.getData().isEmpty());
		assertEquals(1, list.getData().size());
		assertEquals(impossibleName, list.getData().get(0).getLastname());
	}

	@Test
	public void testTokenzierIssueLowercasedQuery() throws Exception {
		String impossibleName = "Jöhä@sRe";
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setLastname(impossibleName);
		call(() -> client().updateUser(userUuid(), updateRequest));

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("lastname.raw", "*" + impossibleName.toLowerCase() + "*")));

		assertNotNull(list);
		assertTrue(
			"No user should be found since the lastname field is not tokenized anymore thus it is not possible to search with a lowercased term.",
			list.getData().isEmpty());
	}

	@Test
	public void testSearchForUserByEmail() throws Exception {
		recreateIndices();
		String email = "testmail@test.com";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername("testuser42a");
		request.setPassword("test1234");
		request.setEmailAddress(email);
		request.setGroupUuid(tx(() -> group().getUuid()));

		call(() -> client().createUser(request));

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleWildCardQuery("emailaddress", "*")));
		assertEquals("We expected to see two results.", 2, list.getData().size());
	}

	@Test
	public void testSearchUserForGroup() throws Exception {
		recreateIndices();
		String username = "extrauser42a";
		String groupName = tx(() -> group().getName());
		createUser(username);

		waitForSearchIdleEvent();

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

		waitForSearchIdleEvent();

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

		waitForSearchIdleEvent();

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
		UserResponse response = call(() -> client().createUser(request));

		String userUuid = response.getUuid();

		call(() -> client().removeUserFromGroup(group.getUuid(), userUuid));

		waitForSearchIdleEvent();

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

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase())));
		assertEquals(0, list.getData().size());
	}

	@Test
	public void testSearchUserWithPerPageZero() throws Exception {
		recreateIndices();
		String groupName = tx(() -> group().getName());
		String username = "extrauser42a";
		createUser(username);

		waitForSearchIdleEvent();

		UserListResponse list = call(
			() -> client().searchUsers(getSimpleTermQuery("groups.name.raw", groupName.toLowerCase()), new PagingParametersImpl().setPerPage(0L)));
		assertEquals(0, list.getData().size());
		assertEquals(2, list.getMetainfo().getTotalCount());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		UserResponse user = createUser(userName);
		call(() -> client().deleteUser(user.getUuid()));

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username", userName)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String userName = "testuser42a";
		String newUserName = "testgrouprenamed";
		UserResponse user = createUser(userName);
		user = updateUser(user.getUuid(), newUserName);

		waitForSearchIdleEvent();

		UserListResponse list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", userName)));
		assertEquals(0, list.getData().size());

		list = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", newUserName)));
		assertEquals(1, list.getData().size());
	}

}

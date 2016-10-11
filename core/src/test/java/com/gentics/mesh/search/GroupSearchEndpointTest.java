package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.rest.client.MeshResponse;

public class GroupSearchEndpointTest extends AbstractSearchEndpointTest implements BasicSearchCrudTestcases {

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		createGroup(groupName);

		MeshResponse<GroupListResponse> searchFuture = getClient().searchGroups(getSimpleTermQuery("name", groupName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	public void testSearchByUuid() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		String uuid = createGroup(groupName).getUuid();

		MeshResponse<GroupListResponse> searchFuture = getClient().searchGroups(getSimpleTermQuery("uuid", uuid)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
		assertEquals(uuid, searchFuture.result().getData().get(0).getUuid());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";

		GroupResponse group = createGroup(groupName);
		deleteGroup(group.getUuid());

		MeshResponse<GroupListResponse> searchFuture = getClient().searchGroups(getSimpleTermQuery("name", groupName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		GroupResponse group = createGroup(groupName);

		String newGroupName = "testgrouprenamed";
		updateGroup(group.getUuid(), newGroupName);

		MeshResponse<GroupListResponse> searchFuture = getClient().searchGroups(getSimpleTermQuery("name", groupName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

		searchFuture = getClient().searchGroups(getSimpleTermQuery("name", newGroupName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

}

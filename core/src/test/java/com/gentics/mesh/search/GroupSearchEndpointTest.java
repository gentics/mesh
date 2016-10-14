package com.gentics.mesh.search;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
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

		GroupListResponse result = call(() -> getClient().searchGroups(getSimpleTermQuery("uuid", uuid)));
		assertThat(result.getData()).hasSize(1);
		assertEquals(uuid, result.getData().get(0).getUuid());
	}

	@Test
	public void testSearchByName() throws InterruptedException, JSONException {
		String groupName = "test-grou  %!p42a";
		String uuid = createGroup(groupName).getUuid();

		GroupListResponse result = call(() -> getClient().searchGroups(getSimpleTermQuery("name", groupName)));
		assertThat(result.getData()).hasSize(1);
		assertEquals(uuid, result.getData().get(0).getUuid());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";

		GroupResponse group = createGroup(groupName);
		deleteGroup(group.getUuid());

		GroupListResponse result = call(() -> getClient().searchGroups(getSimpleTermQuery("name", groupName)));
		assertThat(result.getData()).hasSize(0);
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		GroupResponse group = createGroup(groupName);

		String newGroupName = "testgrouprenamed";
		updateGroup(group.getUuid(), newGroupName);

		GroupListResponse result = call(() -> getClient().searchGroups(getSimpleTermQuery("name", groupName)));
		assertThat(result.getData()).hasSize(0);

		result = call(() -> getClient().searchGroups(getSimpleTermQuery("name", newGroupName)));
		assertThat(result.getData()).hasSize(1);
	}

}

package com.gentics.mesh.search;

import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = TestSize.PROJECT)
public class GroupSearchEndpointTest extends AbstractMeshTest implements BasicSearchCrudTestcases {

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		for (int i = 0; i < 10; i++) {
			createGroup(groupName + i);
		}

		waitForSearchIdleEvent();

		GroupListResponse response = call(() -> client().searchGroups(getSimpleTermQuery("name.raw", groupName + 8)));
		assertEquals(1, response.getData()
				.size());
	}

	@Test
	public void testBogusQuery() {
		String groupName = "testgroup42a";
		String uuid = createGroup(groupName).getUuid();

		waitForSearchIdleEvent();

		// 1. Search with bogus query
		call(() -> client().searchGroups("HudriWudri"), BAD_REQUEST, "search_query_not_parsable");

		// 2. Assert that search still works
		GroupListResponse result = call(() -> client().searchGroups(getSimpleTermQuery("uuid", uuid)));
		assertThat(result.getData()).hasSize(1);
		assertEquals(uuid, result.getData()
				.get(0)
				.getUuid());
	}

	@Test
	public void testSearchByUuid() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		String uuid = createGroup(groupName).getUuid();

		waitForSearchIdleEvent();

		GroupListResponse result = call(() -> client().searchGroups(getSimpleTermQuery("uuid", uuid)));
		assertThat(result.getData()).hasSize(1);
		assertEquals(uuid, result.getData()
				.get(0)
				.getUuid());
	}

	@Test
	public void testSearchByName() throws InterruptedException, JSONException {
		String groupName = "test-grou  %!p42a";
		String uuid = createGroup(groupName).getUuid();

		waitForSearchIdleEvent();

		GroupListResponse result = call(() -> client().searchGroups(getSimpleTermQuery("name.raw", groupName)));
		assertThat(result.getData()).hasSize(1);
		assertEquals(uuid, result.getData()
				.get(0)
				.getUuid());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";

		GroupResponse group = createGroup(groupName);
		deleteGroup(group.getUuid());

		waitForSearchIdleEvent();

		GroupListResponse result = call(() -> client().searchGroups(getSimpleTermQuery("name.raw", groupName)));
		assertThat(result.getData()).hasSize(0);
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String groupName = "testgroup42a";
		GroupResponse group = createGroup(groupName);

		String newGroupName = "testgrouprenamed";
		updateGroup(group.getUuid(), newGroupName);

		waitForSearchIdleEvent();

		GroupListResponse result = call(() -> client().searchGroups(getSimpleTermQuery("name.raw", groupName)));
		assertThat(result.getData()).hasSize(0);

		result = call(() -> client().searchGroups(getSimpleTermQuery("name.raw", newGroupName)));
		assertThat(result.getData()).hasSize(1);
	}

}

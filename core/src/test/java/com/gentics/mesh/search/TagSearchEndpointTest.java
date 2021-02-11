package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = FULL)
public class TagSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public TagSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagName = "newtag";
		try (Tx tx = tx()) {
			createTag(PROJECT_NAME, tagFamily("colors").getUuid(), tagName);
		}

		waitForSearchIdleEvent();

		TagListResponse list = call(() -> client().searchTags(getSimpleTermQuery("name.raw", tagName)));
		assertEquals(1, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String uuid = tx(() -> tag("red").getUuid());
		String parentTagFamilyUuid = tx(() -> tagFamily("colors").getUuid());

		String newName = "redish";
		updateTag(PROJECT_NAME, parentTagFamilyUuid, uuid, newName);
		updateTag(PROJECT_NAME, parentTagFamilyUuid, uuid, newName + "2");

		try (Tx tx = tx()) {
			assertEquals("The tag name was not updated as expected.", newName + "2", tag("red").getName());
		}

		waitForSearchIdleEvent();

		TagListResponse list = call(() -> client().searchTags(getSimpleTermQuery("name.raw", newName + "2")));
		assertEquals(1, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String name = tx(() -> tag("red").getName());
		String uuid = tx(() -> tag("red").getUuid());
		String parentTagFamilyUuid = tx(() -> tagFamily("colors").getUuid());

		waitForSearchIdleEvent();

		// 1. Verify that the tag is indexed
		TagListResponse list = call(() -> client().searchTags(getSimpleTermQuery("name.raw", name)));
		assertEquals("The tag with name {" + name + "} and uuid {" + uuid + "} could not be found in the search index.", 1, list.getData().size());

		// 2. Delete the tag
		call(() -> client().deleteTag(PROJECT_NAME, parentTagFamilyUuid, uuid));

		waitForSearchIdleEvent();

		// 3. Search again and verify that the document was removed from the index
		list = call(() -> client().searchTags(getSimpleTermQuery("fields.name", name)));
		assertEquals(0, list.getData().size());
	}

}

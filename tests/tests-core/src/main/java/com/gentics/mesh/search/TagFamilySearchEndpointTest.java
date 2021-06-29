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

import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = FULL)
public class TagFamilySearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public TagFamilySearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		createTagFamily(PROJECT_NAME, tagFamilyName);

		waitForSearchIdleEvent();
		TagFamilyListResponse list = call(() -> client().searchTagFamilies(getSimpleTermQuery("name.raw", tagFamilyName)));
		assertEquals(1, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamilyResponse = createTagFamily(PROJECT_NAME, tagFamilyName);

		waitForSearchIdleEvent();
		TagFamilyListResponse list = call(() -> client().searchTagFamilies(getSimpleTermQuery("name.raw", tagFamilyName)));
		assertEquals(1, list.getData().size());

		deleteTagFamily(PROJECT_NAME, tagFamilyResponse.getUuid());
		waitForSearchIdleEvent();
		list = call(() -> client().searchTagFamilies(getSimpleTermQuery("name.raw", tagFamilyName)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, tagFamilyName);

		// Update the name of the tag family we just created
		String newTagFamilyName = "updatetagfamilyname";
		updateTagFamily(PROJECT_NAME, tagFamily.getUuid(), newTagFamilyName);

		waitForSearchIdleEvent();

		// Check that the new tag family name is now stored in the search index
		TagFamilyListResponse list = call(() -> client().searchTagFamilies(getSimpleTermQuery("name.raw", newTagFamilyName)));
		assertEquals("The simple term query for name {" + newTagFamilyName + "} did not find the updated tag family entry", 1, list.getData().size());

		// Check that old tag family name is no longer stored in the search index
		list = call(() -> client().searchTagFamilies(getSimpleTermQuery("name.raw", tagFamilyName)));
		assertEquals(
			"The simple term query for name {" + tagFamilyName
				+ "}did find tag families using the old name. Those documents should have been removed from the search index since we updated the tag family name.",
			0, list.getData().size());
	}

}

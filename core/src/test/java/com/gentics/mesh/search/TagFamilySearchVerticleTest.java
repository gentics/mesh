package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.rest.client.MeshResponse;

public class TagFamilySearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagFamilyVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		createTagFamily(PROJECT_NAME, tagFamilyName);

		MeshResponse<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamilyResponse = createTagFamily(PROJECT_NAME, tagFamilyName);

		MeshResponse<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		deleteTagFamily(PROJECT_NAME, tagFamilyResponse.getUuid());
		searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, tagFamilyName);

		//  Update the name of the tag family we just created 
		String newTagFamilyName = "updatetagfamilyname";
		updateTagFamily(PROJECT_NAME, tagFamily.getUuid(), newTagFamilyName);

		// Check that the new tag family name is now stored in the search index
		MeshResponse<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", newTagFamilyName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals("The simple term query for name {" + newTagFamilyName + "} did not find the updated tag family entry", 1,
				searchFuture.result().getData().size());

		// Check that old tag family name is no longer stored in the search index
		searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(
				"The simple term query for name {" + tagFamilyName
						+ "}did find tag families using the old name. Those documents should have been removed from the search index since we updated the tag family name.",
				0, searchFuture.result().getData().size());
	}

}

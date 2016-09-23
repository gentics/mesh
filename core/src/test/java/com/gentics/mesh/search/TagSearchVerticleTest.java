package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.AbstractVerticle;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.searchVerticle());
		list.add(meshDagger.tagFamilyVerticle());
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagName = "newtag";
		try (NoTx noTx = db.noTx()) {
			createTag(PROJECT_NAME, tagFamily("colors").getUuid(), tagName);
		}

		MeshResponse<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", tagName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String tagUuid;
		String parentTagFamilyUuid;
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			TagFamily parentTagFamily = tagFamily("colors");
			tagUuid = tag.getUuid();
			parentTagFamilyUuid = parentTagFamily.getUuid();
		}

		long start = System.currentTimeMillis();
		String newName = "redish";
		updateTag(PROJECT_NAME, parentTagFamilyUuid, tagUuid, newName);
		System.out.println("Took: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();

		updateTag(PROJECT_NAME, parentTagFamilyUuid, tagUuid, newName + "2");
		System.out.println("Took: " + (System.currentTimeMillis() - start));

		try (NoTx noTx = db.noTx()) {

			assertEquals(newName + "2", tag("red").getName());
			assertEquals(0, meshRoot().getSearchQueue().getSize());
		}

		start = System.currentTimeMillis();
		MeshResponse<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", newName + "2")).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
		System.out.println("Took: " + (System.currentTimeMillis() - start));

	}

	@Test
	@Override
	public void testDocumentDeletion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String name;
		String uuid;
		String parentTagFamilyUuid;
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			TagFamily parentTagFamily = tagFamily("colors");

			name = tag.getName();
			uuid = tag.getUuid();
			parentTagFamilyUuid = parentTagFamily.getUuid();
		}

		// 1. Verify that the tag is indexed
		MeshResponse<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals("The tag with name {" + name + "} and uuid {" + uuid + "} could not be found in the search index.", 1,
				searchFuture.result().getData().size());

		// 2. Delete the tag
		deleteTag(PROJECT_NAME, parentTagFamilyUuid, uuid);

		// 3. Search again and verify that the document was removed from the index
		searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

}

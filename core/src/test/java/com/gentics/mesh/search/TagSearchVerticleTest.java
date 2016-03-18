package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.search.index.TagIndexHandler;

import io.vertx.core.Future;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Autowired
	private TagIndexHandler tagIndexHandler;

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
		String tagName = "newtag";
		createTag(PROJECT_NAME, tagFamily("colors").getUuid(), tagName);

		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", tagName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		Tag tag = tag("red");
		TagFamily parentTagFamily = tagFamily("colors");

		long start = System.currentTimeMillis();
		String newName = "redish";
		updateTag(PROJECT_NAME, parentTagFamily.getUuid(), tag.getUuid(), newName);
		System.out.println("Took: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();

		updateTag(PROJECT_NAME, parentTagFamily.getUuid(), tag.getUuid(), newName + "2");
		System.out.println("Took: " + (System.currentTimeMillis() - start));

		assertEquals(newName + "2", tag.getName());
		assertEquals(0, meshRoot().getSearchQueue().getSize());

		start = System.currentTimeMillis();
		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", newName + "2"));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
		System.out.println("Took: " + (System.currentTimeMillis() - start));

	}

	@Test
	@Override
	public void testDocumentDeletion() throws Exception {
		Tag tag = tag("red");
		TagFamily parentTagFamily = tagFamily("colors");

		String name = tag.getName();
		String uuid = tag.getUuid();
		// Add the tag to the index
		tagIndexHandler.store(tag, Tag.TYPE, null).toBlocking().first();
		searchProvider.refreshIndex();

		// 1. Verify that the tag is indexed
		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		// 2. Delete the tag
		deleteTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid);

		// 3. Search again and verify that the document was removed from the index
		searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

}

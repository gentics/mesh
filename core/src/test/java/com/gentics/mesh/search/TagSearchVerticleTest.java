package com.gentics.mesh.search;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.tag.TagVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.search.index.TagIndexHandler;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(TagSearchVerticleTest.class);

	@Autowired
	private TagVerticle tagVerticle;

	@Autowired
	private TagIndexHandler tagIndexHandler;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagName = "newtag";
		createTag(PROJECT_NAME, tagName, "colors");

		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", tagName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		Tag tag = tag("red");

		long start = System.currentTimeMillis();
		String newName = "redish";
		updateTag(PROJECT_NAME, tag.getUuid(), newName);
		System.out.println("Took: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();

		updateTag(PROJECT_NAME, tag.getUuid(), newName + "2");
		System.out.println("Took: " + (System.currentTimeMillis() - start));

		try (Trx tx = db.nonTrx()) {
			assertEquals(newName + "2", tag.getName());
			assertEquals(0, meshRoot().getSearchQueue().getSize());
		}

		start = System.currentTimeMillis();
		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", newName + "2"));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
		System.out.println("Took: " + (System.currentTimeMillis() - start));

	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String name;
		String uuid;
		try (Trx tx = db.trx()) {
			Tag tag = tag("red");
			name = tag.getName();
			uuid = tag.getUuid();
			// Add the tag to the index
			CountDownLatch latch = new CountDownLatch(1);
			tagIndexHandler.store(tag, rh -> {
				latch.countDown();
			});
			failingLatch(latch);
		}
		searchProvider.refreshIndex();

		// 1. Verify that the tag is indexed
		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		// 2. Delete the tag
		deleteTag(PROJECT_NAME, uuid);

		// 3. Search again and verify that the document was removed from the index
		searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

}

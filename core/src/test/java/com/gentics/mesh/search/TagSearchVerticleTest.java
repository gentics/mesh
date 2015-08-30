package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.verticle.tag.ProjectTagVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.search.index.TagIndexHandler;

import io.vertx.core.Future;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private ProjectTagVerticle tagVerticle;

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
	public void testDocumentCreation() throws InterruptedException {
		// TODO the test should be green even when no full index is setup. Update will fail for referenced data, we should fallback to store in those cases?
		// setupFullIndex();

		String tagName = "newtag";

		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setFields(new TagFieldContainer().setName(tagName));
		tagCreateRequest.setTagFamilyReference(new TagFamilyReference().setName("colors"));

		Future<TagResponse> future = getClient().createTag(DemoDataProvider.PROJECT_NAME, tagCreateRequest);
		latchFor(future);
		assertSuccess(future);

		elasticSearchProvider.refreshIndex();

		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", tagName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	public void testDocumentUpdate() throws InterruptedException {
		try (Trx tx = db.trx()) {
			boot.meshRoot().getSearchQueue().addFullIndex();
			tx.success();
		}
		Tag tag = tag("red");

		long start = System.currentTimeMillis();
		String newName = "redish";
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setFields(new TagFieldContainer().setName(newName));

		Future<TagResponse> future = getClient().updateTag(DemoDataProvider.PROJECT_NAME, tag.getUuid(), tagUpdateRequest);
		latchFor(future);
		assertSuccess(future);

		System.out.println("Took: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();

		tagUpdateRequest.setFields(new TagFieldContainer().setName(newName + "2"));
		future = getClient().updateTag(DemoDataProvider.PROJECT_NAME, tag.getUuid(), tagUpdateRequest);
		latchFor(future);
		assertSuccess(future);

		System.out.println("Took: " + (System.currentTimeMillis() - start));

		try (Trx tx = db.trx()) {
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
	public void testDocumentDeletion() throws InterruptedException {
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
		elasticSearchProvider.refreshIndex();

		// 1. Verify that the tag is indexed
		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		// 2. Delete the tag
		Future<GenericMessageResponse> future = getClient().deleteTag(DemoDataProvider.PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);

		// 3. Search again and verify that the document was removed from the index
		searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", name));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

}

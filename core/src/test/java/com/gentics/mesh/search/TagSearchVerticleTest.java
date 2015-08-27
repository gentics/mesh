package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.verticle.tag.ProjectTagVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Future;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private ProjectTagVerticle tagVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagVerticle);
		return list;
	}

	@Test
	public void testDocumentCreation() {

	}

	@Test
	public void testDocumentUpdate() {
		Tag tag = tag("red");

		String newName = "redish";
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setFields(new TagFieldContainer().setName(newName));

		Future<TagResponse> future = getClient().updateTag(DemoDataProvider.PROJECT_NAME, tag.getUuid(), tagUpdateRequest);
		latchFor(future);
		assertSuccess(future);

		try (Trx tx = db.trx()) {
			assertEquals(newName, tag.getName());
		}

		Future<TagListResponse> searchFuture = getClient().searchTags(getSimpleTermQuery("fields.name", newName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

	}

	@Test
	public void testDocumentDeletion() {

	}

}

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

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;

import io.vertx.core.Future;

public class TagFamilySearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagFamilyVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		createTagFamily(PROJECT_NAME, tagFamilyName);

		Future<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamilyResponse = createTagFamily(PROJECT_NAME, tagFamilyName);

		Future<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		deleteTagFamily(PROJECT_NAME, tagFamilyResponse.getUuid());
		searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, tagFamilyName);

		String newTagFamilyName = "updatetagfamilyname";
		updateTagFamily(PROJECT_NAME, tagFamily.getUuid(), newTagFamilyName);

		Future<TagFamilyListResponse> searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", newTagFamilyName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		searchFuture = getClient().searchTagFamilies(getSimpleTermQuery("name", tagFamilyName));
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());
	}

}

package com.gentics.mesh.core.tag;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class TagVerticleETagTest extends AbstractETagTest {

	private TagFamilyVerticle verticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			String tagFamilyUuid = tagFamily("colors").getUuid();
			MeshResponse<TagListResponse> response = getClient().findTags(PROJECT_NAME, tagFamilyUuid).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findTags(PROJECT_NAME, tagFamilyUuid), etag, true);
			expectNo304(getClient().findTags(PROJECT_NAME, tagFamilyUuid, new PagingParameters().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagfamily = tagFamily("colors");
			Tag tag = tag("red");

			MeshResponse<TagResponse> response = getClient().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid()).invoke();
			latchFor(response);
			String etag = tag.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<TagResponse> request = getClient().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag,
					expect304(getClient().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParameters().setExpandAll(true)),
							etag, true));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParameters().setExpandAll(false)), etag,
					true);
			expect304(getClient().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParameters().setExpandAll(true)), etag,
					true);
		}

	}

}

package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;

public class TagFamilyVerticleETagTest extends AbstractETagTest {

	@Autowired
	private TagFamilyVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Override
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<TagFamilyListResponse> response = getClient().findTagFamilies(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = response.getResponse().getHeader(ETAG);
			assertNotNull(etag);

			expect304(getClient().findTagFamilies(PROJECT_NAME), etag);
			expectNo304(getClient().findTagFamilies(PROJECT_NAME, new PagingParameters().setPage(2)), etag);
		}
	}

	@Test
	@Override
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagfamily = tagFamily("colors");

			MeshResponse<TagFamilyResponse> response = getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()).invoke();
			latchFor(response);
			String etag = tagfamily.getETag(getMockedInternalActionContext());
			assertEquals(etag, response.getResponse().getHeader(ETAG));

			// Check whether 304 is returned for correct etag
			MeshRequest<TagFamilyResponse> request = getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid());
			assertEquals(etag, expect304(request, etag));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag,
					expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)), etag));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(false)), etag);
			expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)), etag);
		}

	}

}

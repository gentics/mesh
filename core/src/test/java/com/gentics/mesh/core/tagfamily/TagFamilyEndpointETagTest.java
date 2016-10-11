package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class TagFamilyEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<TagFamilyListResponse> response = getClient().findTagFamilies(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findTagFamilies(PROJECT_NAME), etag, true);
			expectNo304(getClient().findTagFamilies(PROJECT_NAME, new PagingParameters().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagfamily = tagFamily("colors");

			MeshResponse<TagFamilyResponse> response = getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()).invoke();
			latchFor(response);
			String etag = tagfamily.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<TagFamilyResponse> request = getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)),
					etag, true));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(getClient().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}

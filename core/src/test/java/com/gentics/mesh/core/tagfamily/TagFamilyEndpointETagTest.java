package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagFamilyEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = db().tx()) {
			MeshResponse<TagFamilyListResponse> response = client().findTagFamilies(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getRawResponse().getHeader(ETAG));
			assertNotNull(etag);

			callETag(() -> client().findTagFamilies(PROJECT_NAME), etag, true, 304);
			callETag(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = db().tx()) {
			TagFamily tagfamily = tagFamily("colors");

			MeshResponse<TagFamilyResponse> response = client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()).invoke();
			latchFor(response);
			String etag = tagfamily.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getRawResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()), etag, true, 304));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag,
					callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag,
							true, 304));

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true,
					304);
			callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true,
					304);
		}

	}

}

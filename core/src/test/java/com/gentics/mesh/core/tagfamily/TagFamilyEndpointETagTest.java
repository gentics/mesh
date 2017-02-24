package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagFamilyEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db().noTx()) {
			MeshResponse<TagFamilyListResponse> response = client().findTagFamilies(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findTagFamilies(PROJECT_NAME), etag, true);
			expectNo304(client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db().noTx()) {
			TagFamily tagfamily = tagFamily("colors");

			MeshResponse<TagFamilyResponse> response = client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()).invoke();
			latchFor(response);
			String etag = tagfamily.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<TagFamilyResponse> request = client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag,
					expect304(client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)), etag, true));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}

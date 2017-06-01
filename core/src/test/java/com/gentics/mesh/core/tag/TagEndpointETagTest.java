package com.gentics.mesh.core.tag;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = db().tx()) {
			String tagFamilyUuid = tagFamily("colors").getUuid();
			String etag = callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid));
			assertNotNull(etag);

			callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid), etag, true, 304);
			callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = db().tx()) {
			TagFamily tagfamily = tagFamily("colors");
			Tag tag = tag("red");

			String actualEtag = callETag(() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid()));
			String etag = tag.getETag(mockActionContext());
			assertEquals(etag, actualEtag);

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid()), etag, true, 304));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, callETag(
					() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag,
					true, 304));

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag,
					true, 304);
			callETag(() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag,
					true, 304);
		}

	}

}

package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagFamilyEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findTagFamilies(PROJECT_NAME));
			callETag(() -> client().findTagFamilies(PROJECT_NAME), etag, true, 304);
			callETag(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			TagFamily tagfamily = tagFamily("colors");

			String actualEtag = callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()));
			String etag = tagfamily.getETag(mockActionContext());
			assertEquals(etag, actualEtag);

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

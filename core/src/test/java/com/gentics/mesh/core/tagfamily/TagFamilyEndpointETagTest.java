package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
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
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();

			HibTagFamily tagfamily = tagFamily("colors");

			String actualEtag = callETag(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagfamily.getUuid()));
			String etag = tagFamilyDao.getETag(tagfamily, mockActionContext());
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

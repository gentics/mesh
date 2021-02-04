package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.OrientDBTagDao;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class TagEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String tagFamilyUuid = tagFamily("colors").getUuid();
			String etag = callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid));

			callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid), etag, true, 304);
			callETag(() -> client().findTags(PROJECT_NAME, tagFamilyUuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			OrientDBTagDao tagDao = tx.tagDao();
			HibTagFamily tagfamily = tagFamily("colors");
			HibTag tag = tag("red");

			String actualEtag = callETag(() -> client().findTagByUuid(PROJECT_NAME, tagfamily.getUuid(), tag.getUuid()));
			String etag = tagDao.getETag(tag, mockActionContext());
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

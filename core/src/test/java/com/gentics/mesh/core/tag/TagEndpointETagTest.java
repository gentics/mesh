package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
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

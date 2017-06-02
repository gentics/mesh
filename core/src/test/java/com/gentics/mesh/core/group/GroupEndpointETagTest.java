package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class GroupEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findGroups());
			assertNotNull(etag);

			callETag(() -> client().findGroups(), etag, true, 304);
			callETag(() -> client().findGroups(new PagingParametersImpl().setPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			Group group = group();

			String actualEtag = callETag(() -> client().findGroupByUuid(group.getUuid()));
			String etag = group.getETag(mockActionContext());
			assertEquals(etag, actualEtag);

			// Check whether 304 is returned for correct etag
			callETag(() -> client().findGroupByUuid(group.getUuid()), etag, true, 304);

			// The node has no node reference and thus expanding will not affect the etag
			callETag(() -> client().findGroupByUuid(group.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);

			// Assert that adding bogus query parameters will not affect the etag
			callETag(() -> client().findGroupByUuid(group.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304);
			callETag(() -> client().findGroupByUuid(group.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304);
		}

	}

}

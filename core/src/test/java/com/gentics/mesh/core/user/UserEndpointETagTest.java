package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class UserEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			User user = user();
			assertNotNull("The UUID of the user must not be null.", user.getUuid());

			String etag = callETag(() -> client().findUsers());
			callETag(() -> client().findUsers(), etag, true, 304);
			callETag(() -> client().findUsers(new PagingParametersImpl().setPage(2)), etag, true, 200);

		}
	}

	@Test
	public void testReadOne() {
		String etag;
		try (Tx tx = tx()) {
			User user = user();

			etag = user().getETag(mockActionContext());
			callETag(() -> client().findUserByUuid(user.getUuid()), etag, true, 304);

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().findUserByUuid(user.getUuid()), etag, true, 304));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, callETag(() -> client().findUserByUuid(user.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304));

			// Add node reference. This should affect the etag
			user.setReferencedNode(content());
			tx.success();
		}
		String newETag = callETag(() -> client().findUserByUuid(userUuid()), etag, true, 200);

		// Assert whether expanding the user will also affect the user etag
		callETag(() -> client().findUserByUuid(userUuid(), new NodeParametersImpl().setExpandAll(false)), newETag, true, 304);
		callETag(() -> client().findUserByUuid(userUuid(), new NodeParametersImpl().setExpandAll(true)), newETag, true, 200);

	}

}

package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.ClientHelper.callETagRaw;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class UserEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findUsers());
			callETag(() -> client().findUsers(), etag, true, 304);
			callETag(() -> client().findUsers(new PagingParametersImpl().setPage(2)), etag, true, 200);

		}
	}

	@Test
	public void testReadWithoutETag() {
		String etag = callETagRaw(() -> client().findUsers(new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);

		etag = callETagRaw(() -> client().findUserByUuid(userUuid(), new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);
	}

	@Test
	public void testEtagPermissionHandling() {
		String etag = callETag(() -> client().findUsers());
		String etag2 = callETag(() -> client().findUsers());
		callETag(() -> client().findUsers(), etag, true, 304);
		assertEquals(etag, etag2);

		try (Tx tx = tx()) {
			role().revokePermissions(user(), UPDATE_PERM);
			tx.success();
		}

		callETag(() -> client().findUsers(), etag, true, 200);

		String etag3 = callETag(() -> client().findUsers());
		assertNotEquals(etag, etag3);
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

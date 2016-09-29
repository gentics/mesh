package com.gentics.mesh.core.user;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class UserVerticleETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			assertNotNull("The UUID of the user must not be null.", user.getUuid());

			MeshResponse<UserListResponse> response = getClient().findUsers().invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findUsers(), etag, true);
			expectNo304(getClient().findUsers(new PagingParameters().setPage(2)), etag, true);

		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			assertNotNull("The UUID of the user must not be null.", user.getUuid());

			MeshResponse<UserResponse> response = getClient().findUserByUuid(user.getUuid()).invoke();
			latchFor(response);
			String etag = user().getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<UserResponse> request = getClient().findUserByUuid(user.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, expect304(getClient().findUserByUuid(user.getUuid(), new NodeParameters().setExpandAll(true)), etag, true));

			// Add node reference. This should affect the etag
			user.setReferencedNode(content());
			request = getClient().findUserByUuid(user.getUuid());
			String newETag = expectNo304(request, etag, true);
			assertNotEquals(etag, newETag);

			// Assert whether expanding the node will also affect the user etag
			expect304(getClient().findUserByUuid(user.getUuid(), new NodeParameters().setExpandAll(false)), newETag, true);
			expectNo304(getClient().findUserByUuid(user.getUuid(), new NodeParameters().setExpandAll(true)), newETag, true);
		}

	}

}

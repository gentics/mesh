package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.ClientHelper.callETagRaw;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
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
	public void testReadWithoutETag() {
		String etag = callETagRaw(() -> client().findGroups(new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);

		etag = callETagRaw(() -> client().findGroupByUuid(groupUuid(), new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);
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

	/**
	 * Link a user to a group and check whether this affects the etag of user pages (since the user in the page contains group information).
	 */
	@Test
	public void testLinkingEtagHandling() {
		String groupUuid = groupUuid();
		UserResponse user2 = call(() -> client().createUser(new UserCreateRequest().setUsername("someUser").setPassword("test")));
		String userUuid = user2.getUuid();

		String before = callETag(() -> client().findUsers());
		call(() -> client().addUserToGroup(groupUuid, userUuid));
		String after = callETag(() -> client().findUsers());
		assertNotEquals("Adding the user should have changed the etag.", before, after);
	}

}

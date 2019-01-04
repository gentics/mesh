package com.gentics.mesh.core.role;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.ClientHelper.callETagRaw;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class RoleEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String actualEtag = callETag(() -> client().findRoles());
			assertNotNull(actualEtag);

			callETag(() -> client().findRoles(), actualEtag, true, 304);
			callETag(() -> client().findRoles(new PagingParametersImpl().setPage(2)), actualEtag, true, 200);
		}
	}

	@Test
	public void testReadWithoutETag() {
		String etag = callETagRaw(() -> client().findRoles(new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);

		etag = callETagRaw(() -> client().findRoleByUuid(roleUuid(), new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);
	}

	@Test
	public void testReadOne() {
		try (Tx tx = tx()) {
			Role role = role();
			MeshResponse<RoleResponse> response = client().findRoleByUuid(role.getUuid()).invoke();
			latchFor(response);
			String etag = role.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getRawResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().findRoleByUuid(role.getUuid()), etag, true, 304));

			// The role has no node reference and thus expanding will not affect the etag
			assertEquals(etag, callETag(() -> client().findRoleByUuid(role.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304));

			// Assert that adding bogus query parameters will not affect the etag
			assertEquals(etag,
				callETag(() -> client().findRoleByUuid(role.getUuid(), new NodeParametersImpl().setExpandAll(false)), etag, true, 304));
			assertEquals(etag, callETag(() -> client().findRoleByUuid(role.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 304));
		}

	}

}

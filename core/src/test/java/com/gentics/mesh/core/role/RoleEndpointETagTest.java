package com.gentics.mesh.core.role;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class RoleEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<RoleListResponse> response = client().findRoles().invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findRoles(), etag, true);
			expectNo304(client().findRoles(new PagingParametersImpl().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			MeshResponse<RoleResponse> response = client().findRoleByUuid(role.getUuid()).invoke();
			latchFor(response);
			String etag = role.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<RoleResponse> request = client().findRoleByUuid(role.getUuid());
			assertEquals(etag, expect304(request, etag, true));

			// The role has no node reference and thus expanding will not affect the etag
			assertEquals(etag, expect304(client().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(true)), etag, true));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(client().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(client().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}

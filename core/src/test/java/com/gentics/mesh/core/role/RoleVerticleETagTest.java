package com.gentics.mesh.core.role;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;

public class RoleVerticleETagTest extends AbstractETagTest {

	@Autowired
	private RoleVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Override
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<RoleListResponse> response = getClient().findRoles().invoke();
			latchFor(response);
			String etag = response.getResponse().getHeader(ETAG);
			assertNotNull(etag);

			expect304(getClient().findRoles(), etag);
			expectNo304(getClient().findRoles(new PagingParameters().setPage(2)), etag);
		}
	}

	@Test
	@Override
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			MeshResponse<RoleResponse> response = getClient().findRoleByUuid(role.getUuid()).invoke();
			latchFor(response);
			String etag = role.getETag(getMockedInternalActionContext());
			assertEquals(etag, response.getResponse().getHeader(ETAG));

			// Check whether 304 is returned for correct etag
			MeshRequest<RoleResponse> request = getClient().findRoleByUuid(role.getUuid());
			assertEquals(etag, expect304(request, etag));

			// The node has no node reference and thus expanding will not affect the etag
			assertEquals(etag, expect304(getClient().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(true)), etag));

			// Assert that adding bogus query parameters will not affect the etag
			expect304(getClient().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(false)), etag);
			expect304(getClient().findRoleByUuid(role.getUuid(), new NodeParameters().setExpandAll(true)), etag);
		}

	}

}

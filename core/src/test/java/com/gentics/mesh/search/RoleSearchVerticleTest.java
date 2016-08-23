package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.rest.client.MeshResponse;

public class RoleSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	private RoleVerticle roleVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(roleVerticle);
		return list;
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		createRole(roleName, db.noTx(() -> group().getUuid()));

		MeshResponse<RoleListResponse> searchFuture = getClient().searchRoles(getSimpleTermQuery("name", roleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db.noTx(() -> group().getUuid()));

		MeshResponse<RoleListResponse> searchFuture = getClient().searchRoles(getSimpleTermQuery("name", roleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		deleteRole(role.getUuid());

		searchFuture = getClient().searchRoles(getSimpleTermQuery("name", roleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db.noTx(() -> group().getUuid()));

		MeshResponse<RoleListResponse> searchFuture = getClient().searchRoles(getSimpleTermQuery("name", roleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		String newRoleName = "updatedrolename";
		updateRole(role.getUuid(), newRoleName);

		searchFuture = getClient().searchRoles(getSimpleTermQuery("name", newRoleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(1, searchFuture.result().getData().size());

		searchFuture = getClient().searchRoles(getSimpleTermQuery("name", roleName)).invoke();
		latchFor(searchFuture);
		assertSuccess(searchFuture);
		assertEquals(0, searchFuture.result().getData().size());

	}
}

package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.PROJECT)
public class RoleSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public RoleSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		createRole(roleName, db().tx(() -> group().getUuid()));

		waitForSearchIdleEvent();

		RoleListResponse list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", roleName)));
		assertEquals(1, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db().tx(() -> group().getUuid()));

		waitForSearchIdleEvent();

		RoleListResponse list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", roleName)));
		assertEquals(1, list.getData().size());

		deleteRole(role.getUuid());

		waitForSearchIdleEvent();

		list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", roleName)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db().tx(() -> group().getUuid()));

		waitForSearchIdleEvent();

		RoleListResponse list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", roleName)));
		assertEquals(1, list.getData().size());

		String newRoleName = "updatedrolename";
		updateRole(role.getUuid(), newRoleName);

		waitForSearchIdleEvent();

		list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", newRoleName)));
		assertEquals(1, list.getData().size());

		list = call(() -> client().searchRoles(getSimpleTermQuery("name.raw", roleName)));
		assertEquals(0, list.getData().size());

	}
}

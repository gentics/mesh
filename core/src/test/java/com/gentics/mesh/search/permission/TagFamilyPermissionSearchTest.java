package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class TagFamilyPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String tagfamilyname = "testtagfamily42a";
		TagFamilyResponse response = createTagFamily(PROJECT_NAME, tagfamilyname);
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findByUuid(response.getUuid());
			System.out.println("TagFamily Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.revokePermissions(tagFamily, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		String json = getESText("tagFamilyWildcard.es");

		TagFamilyListResponse list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findByUuid(response.getUuid());
			System.out.println("TagFamily Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.grantPermissions(tagFamily, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String tagfamilyName = "testtagfamily42a";
		TagFamilyResponse response = createTagFamily(PROJECT_NAME, tagfamilyName);

		String json = getESText("tagFamilyWildcard.es");

		TagFamilyListResponse list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/tagFamilies/" + response.getUuid(), request));

		list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}

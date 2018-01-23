package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.FULL, startServer = true)
public class TagPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String tagname = "testtag42a";
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		TagResponse response = createTag(PROJECT_NAME, tagFamilyUuid, tagname);
		try (Tx tx = tx()) {
			Tag tag = tagFamily("colors").findByUuid(response.getUuid());
			System.out.println("Tag Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.revokePermissions(tag, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		String json = getESText("tagWildcard.es");

		TagListResponse list = call(() -> client().searchTags(json));
		assertEquals("The tag should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			Tag tag = tagFamily("colors").findByUuid(response.getUuid());
			System.out.println("Tag Uuid:" + response.getUuid());
			for (Role role : user().getRoles()) {
				role.grantPermissions(tag, GraphPermission.READ_PERM);
			}
			tx.success();
		}

		try (Tx tx = tx()) {
			recreateIndices();
		}

		list = call(() -> client().searchTags(json));
		assertEquals("The tag should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String tagname = "testtag42a";
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		TagResponse response = createTag(PROJECT_NAME, tagFamilyUuid, tagname);

		String json = getESText("tagWildcard.es");

		TagListResponse list = call(() -> client().searchTags(json));
		assertEquals("The tag should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/tagFamilies/" + tagFamilyUuid + "/tags/" + response.getUuid(), request));

		list = call(() -> client().searchTags(json));
		assertEquals("The tag should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}

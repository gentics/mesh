package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class TagPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String tagname = "testtag42a";
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		TagResponse response = createTag(PROJECT_NAME, tagFamilyUuid, tagname);
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			TagDao tagDao = tx.tagDao();
			UserDao userDao = tx.userDao();

			HibTag tag = tagDao.findByUuid(tagFamily("colors"), response.getUuid());
			System.out.println("Tag Uuid:" + response.getUuid());
			for (HibRole role : userDao.getRoles(user())) {
				roleDao.revokePermissions(role, tag, InternalPermission.READ_PERM);
			}
			tx.success();
		}

		recreateIndices();

		String json = getESText("tagWildcard.es");

		TagListResponse list = call(() -> client().searchTags(json));
		assertEquals("The tag should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			RoleDao roleDao = tx.roleDao();
			TagDao tagDao = tx.tagDao();

			HibTag tag = tagDao.findByUuid(tagFamily("colors"), response.getUuid());
			System.out.println("Tag Uuid:" + response.getUuid());
			for (HibRole role : userDao.getRoles(user())) {
				roleDao.grantPermissions(role, tag, InternalPermission.READ_PERM);
			}
			tx.success();
		}

		recreateIndices();

		list = call(() -> client().searchTags(json));
		assertEquals("The tag should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		String tagname = "testtag42a";
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		TagResponse response = createTag(PROJECT_NAME, tagFamilyUuid, tagname);

		String json = getESText("tagWildcard.es");

		waitForSearchIdleEvent();

		TagListResponse list = call(() -> client().searchTags(json));
		assertEquals("The tag should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/tagFamilies/" + tagFamilyUuid + "/tags/" + response.getUuid(), request));

		waitForSearchIdleEvent();

		list = call(() -> client().searchTags(json));
		assertEquals("The tag should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}

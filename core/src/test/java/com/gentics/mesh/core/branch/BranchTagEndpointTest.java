package com.gentics.mesh.core.branch;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UNTAGGED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchTagEndpointTest extends AbstractMeshTest {

	@Test
	public void testAddTagToBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		try (Tx tx = tx()) {
			assertFalse(branch.getTags().list().contains(tag));
		}

		expect(BRANCH_TAGGED).match(1, BranchTaggedEventModel.class, event -> {
			BranchReference branchRef = event.getBranch();
			assertNotNull(branchRef);
			assertEquals(PROJECT_NAME, branchRef.getName());
			assertEquals(initialBranchUuid(), branchRef.getUuid());

			TagReference tagRef = event.getTag();
			assertNotNull(tagRef);
			assertEquals("The tag name in the event did not match.", "red", tagRef.getName());
			assertEquals("The tag uuid in the event did not match.", tagUuid, tagRef.getUuid());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());
		});

		BranchResponse branchResponse = call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid));
		awaitEvents();

		try (Tx tx = tx()) {
			assertThat(branchResponse).contains(tag);
			assertTrue(branch.getTags().list().contains(tag));
		}
	}

	@Test
	public void testAddTagToNoPermBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			assertFalse(branch.getTags().list().contains(tag));
			roleDao.revokePermissions(role(), branch, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid), FORBIDDEN, "error_missing_perm", branchUuid,
			UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertFalse(branch.getTags().list().contains(tag));
		}
	}

	@Test
	public void testAddNoPermTagToBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			assertFalse(branch.getTags().list().contains(tag));
			roleDao.revokePermissions(role(), tag, READ_PERM);
			tx.success();
		}

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid), FORBIDDEN, "error_missing_perm", tagUuid,
			READ_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertFalse(branch.getTags().list().contains(tag));
		}
	}

	@Test
	public void testAddBogusTagToBranch() throws Exception {
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddTagToBogusBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, "bogus", tagUuid), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testRemoveTagFromBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid));
		try (Tx tx = tx()) {
			assertTrue(branch.getTags().list().contains(tag));
		}

		expect(BRANCH_UNTAGGED).match(1, BranchTaggedEventModel.class, event -> {
			BranchReference branchRef = event.getBranch();
			assertNotNull(branchRef);
			assertEquals(PROJECT_NAME, branchRef.getName());
			assertEquals(initialBranchUuid(), branchRef.getUuid());

			TagReference tagRef = event.getTag();
			assertNotNull(tagRef);
			assertEquals("The tag name in the event did not match.", "red", tagRef.getName());
			assertEquals("The tag uuid in the event did not match.", tagUuid, tagRef.getUuid());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());
		});
		call(() -> client().removeTagFromBranch(PROJECT_NAME, branchUuid, tagUuid));
		// Test idempotency
		call(() -> client().removeTagFromBranch(PROJECT_NAME, branchUuid, tagUuid));
		awaitEvents();
		try (Tx tx = tx()) {
			assertFalse(branch.getTags().list().contains(tag));
		}

	}

	@Test
	public void testRemoveTagFromNoPermBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid));
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			assertTrue(branch.getTags().list().contains(tag));
			roleDao.revokePermissions(role(), branch, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid), FORBIDDEN, "error_missing_perm", branchUuid,
			UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertTrue(branch.getTags().list().contains(tag));
		}
	}

	@Test
	public void testRemoveNoPermTagFromBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid));
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			assertTrue(branch.getTags().list().contains(tag));
			roleDao.revokePermissions(role(), tag, READ_PERM);
			tx.success();
		}

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, tagUuid), FORBIDDEN, "error_missing_perm", tagUuid,
			READ_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertTrue(branch.getTags().list().contains(tag));
		}
	}

	@Test
	public void testRemoveBogusTagFromBranch() throws Exception {
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().removeTagFromBranch(PROJECT_NAME, branchUuid, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testRemoveTagFromBogusBranch() throws Exception {
		HibTag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());

		call(() -> client().removeTagFromBranch(PROJECT_NAME, "bogus", tagUuid), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testReadTagsFromBranch() throws Exception {
		HibTag red = tag("red");
		String redUuid = tx(() -> red.getUuid());
		HibTag blue = tag("blue");
		String blueUuid = tx(() -> blue.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, redUuid));
		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, blueUuid));

		TagListResponse tagListResponse = call(() -> client().findTagsForBranch(PROJECT_NAME, branchUuid));
		assertThat(tagListResponse).containsExactly("red", "blue");
	}

	@Test
	public void testReadNoPermTagsFromBranch() throws Exception {
		HibTag red = tag("red");
		String redUuid = tx(() -> red.getUuid());
		HibTag blue = tag("blue");
		String blueUuid = tx(() -> blue.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, redUuid));
		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, blueUuid));

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			roleDao.revokePermissions(role(), red, READ_PERM);
			tx.success();
		}

		TagListResponse tagListResponse = call(() -> client().findTagsForBranch(PROJECT_NAME, branchUuid));
		assertThat(tagListResponse).containsExactly("blue");
	}

	@Test
	public void testReadTagsFromNoPermBranch() throws Exception {
		HibTag red = tag("red");
		String redUuid = tx(() -> red.getUuid());
		HibTag blue = tag("blue");
		String blueUuid = tx(() -> blue.getUuid());
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, redUuid));
		call(() -> client().addTagToBranch(PROJECT_NAME, branchUuid, blueUuid));

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			roleDao.revokePermissions(role(), branch, READ_PERM);
			tx.success();
		}

		call(() -> client().findTagsForBranch(PROJECT_NAME, branchUuid), FORBIDDEN, "error_missing_perm", branchUuid,
			READ_PERM.getRestPerm().getName());
	}

	@Test
	public void testReadTagsFromBogusBranch() throws Exception {
		call(() -> client().findTagsForBranch(PROJECT_NAME, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testReadNoTags() throws Exception {
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());
		BranchResponse response = call(
			() -> client().findBranchByUuid(PROJECT_NAME, branchUuid, new GenericParametersImpl().setFields("uuid", "name")));
		assertThat(response.getTags()).as("Tags").isNull();
	}

	@Test
	public void testReadWithTags() throws Exception {
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());
		BranchResponse response = call(
			() -> client().findBranchByUuid(PROJECT_NAME, branchUuid, new GenericParametersImpl().setFields("uuid", "name", "tags")));
		assertThat(response.getTags()).as("Tags").isNotNull();
	}

	@Test
	public void testUpdateTagsForBranch() throws Exception {
		HibTag red = tag("red");
		HibTag blue = tag("blue");
		HibTag car = tag("car");
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		TagListResponse tagListResponse = call(
			() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(red), ref(car)))));
		assertThat(tagListResponse).containsExactly("red", "Car");

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("red", "Car");
		}

		tagListResponse = call(
			() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(blue), ref(car)))));
		assertThat(tagListResponse).containsExactly("blue", "Car");

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("blue", "Car");
		}
	}

	@Test
	public void testUpdateNoPermTagsForBranch() throws Exception {
		HibTag red = tag("red");
		HibTag blue = tag("blue");
		String blueUuid = tx(() -> blue.getUuid());
		HibTag car = tag("car");
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			roleDao.revokePermissions(role(), blue, READ_PERM);
			tx.success();
		}

		TagListResponse tagListResponse = call(
			() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(red), ref(car)))));
		assertThat(tagListResponse).containsExactly("red", "Car");

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("red", "Car");
		}

		call(() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(blue), ref(car)))),
			FORBIDDEN,
			"error_missing_perm", blueUuid, READ_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("red", "Car");
		}
	}

	@Test
	public void testUpdateTagsForNoPermBranch() throws Exception {
		HibTag red = tag("red");
		HibTag blue = tag("blue");
		HibTag car = tag("car");
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		TagListResponse tagListResponse = call(
			() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(red), ref(car)))));
		assertThat(tagListResponse).containsExactly("red", "Car");

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("red", "Car");
		}

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			roleDao.revokePermissions(role(), branch, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid, new TagListUpdateRequest().setTags(Arrays.asList(ref(blue), ref(car)))),
			FORBIDDEN,
			"error_missing_perm", branchUuid, UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			assertThat(branch).isOnlyTagged("red", "Car");
		}
	}

	@Test
	public void testUpdateTagsForBogusBranch() throws Exception {
		HibTag red = tag("red");
		HibTag car = tag("car");

		call(() -> client().updateTagsForBranch(PROJECT_NAME, "bogus", new TagListUpdateRequest().setTags(Arrays.asList(ref(red), ref(car)))),
			NOT_FOUND,
			"object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testUpdateBogusTagsForBranch() throws Exception {
		HibTag red = tag("red");
		HibBranch branch = tx(() -> project().getLatestBranch());
		String branchUuid = tx(() -> branch.getUuid());

		call(
			() -> client().updateTagsForBranch(PROJECT_NAME, branchUuid,
				new TagListUpdateRequest().setTags(Arrays.asList(ref(red), ref("bogus", "colors")))),
			NOT_FOUND, "tag_not_found", "bogus");

	}

	/**
	 * Create a tag reference to the tag (using the uuid)
	 * 
	 * @param tag
	 *            tag
	 * @return tag reference
	 */
	protected TagReference ref(HibTag tag) {
		return tx(() -> ref(tag.getUuid(), tag.getTagFamily().getName()));
	}

	/**
	 * Create a tag reference to the tagfamily and tag uuid
	 * 
	 * @param uuid
	 *            tag uuid
	 * @param tagFamilyName
	 *            name of the tag family
	 * @return tag reference
	 */
	protected TagReference ref(String uuid, String tagFamilyName) {
		return new TagReference().setUuid(uuid).setTagFamily(tagFamilyName);
	}
}

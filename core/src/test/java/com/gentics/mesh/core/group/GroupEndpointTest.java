package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.ClientHelper.validateSet;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.MeshTestHelper.prepareBarrier;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class GroupEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	private static final Logger log = LoggerFactory.getLogger(GroupEndpointTest.class);

	@Test
	@Override
	public void testCreate() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("test12345");

		GroupResponse restGroup = call(() -> client().createGroup(request));
		assertThat(restGroup).matches(request);

		assertThat(trackingSearchProvider()).hasStore(Group.composeIndexName(), restGroup.getUuid());
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);
		trackingSearchProvider().clear().blockingAwait();

		try (Tx tx = tx()) {
			assertElement(boot().groupRoot(), restGroup.getUuid(), true);
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("test12345");
		String groupRootUuid = db().tx(() -> meshRoot().getGroupRoot().getUuid());

		try (Tx tx = tx()) {
			role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
			tx.success();
		}

		call(() -> client().createGroup(request), FORBIDDEN, "error_missing_perm", groupRootUuid);
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		final String name = "New Name";
		String uuid = UUIDUtil.randomUUID();

		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		GroupResponse restGroup = call(() -> client().updateGroup(uuid, request));
		assertThat(trackingSearchProvider()).hasStore(Group.composeIndexName(), uuid);
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);

		try (Tx tx = db().tx()) {
			assertThat(restGroup).matches(request);
			Group reloadedGroup = boot().groupRoot().findByUuid(uuid);
			assertEquals("The group should have been updated", name, reloadedGroup.getName());
		}
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		final String name = "New Name";
		String uuid = projectUuid();

		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		call(() -> client().updateGroup(uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
	}

	@Test
	public void testBatchCreation() {
		try (Tx tx = tx()) {
			GroupRoot root = meshRoot().getGroupRoot();
			role().grantPermissions(root, CREATE_PERM);
			tx.success();
		}
		for (int i = 0; i < 10; i++) {
			System.out.println(i);
			final String name = "test_" + i;
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);
			GroupResponse restGroup = call(() -> client().createGroup(request));
			assertThat(restGroup).matches(request);
		}
	}

	@Test
	public void testConflicingGroupCreation() throws Exception {
		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);

		try (Tx tx = tx()) {
			role().grantPermissions(meshRoot().getGroupRoot(), CREATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupResponse restGroup = call(() -> client().createGroup(request));
			assertThat(restGroup).matches(request);

			assertElement(boot().groupRoot(), restGroup.getUuid(), true);
			call(() -> client().createGroup(request), CONFLICT, "group_conflicting_name", name);
		}
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (Tx tx = tx()) {
			// Create the group
			final String name = "test12345";
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);

			GroupResponse restGroup = call(() -> client().createGroup(request));
			assertThat(restGroup).matches(request);

			Group foundGroup = boot().groupRoot().findByUuid(restGroup.getUuid());
			assertNotNull("Group should have been created.", foundGroup);

			call(() -> client().findGroupByUuid(restGroup.getUuid()));

			// Now delete the group
			call(() -> client().deleteGroup(restGroup.getUuid()));
		}
	}

	@Test
	public void testCreateGroupWithMissingName() throws Exception {
		try (Tx tx = tx()) {
			role().grantPermissions(group(), CREATE_PERM);
			tx.success();
		}
		GroupCreateRequest request = new GroupCreateRequest();
		call(() -> client().createGroup(request), BAD_REQUEST, "error_name_must_be_set");

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		try (Tx tx = tx()) {
			final String name = "test12345";
			request.setName(name);
			GroupRoot root = meshRoot().getGroupRoot();
			role().revokePermissions(root, CREATE_PERM);
			User user = user();
			assertFalse("The create permission to the groups root node should have been revoked.", user.hasPermission(root, CREATE_PERM));
			tx.success();
		}
		String rootUuid = db().tx(() -> meshRoot().getGroupRoot().getUuid());
		call(() -> client().createGroup(request), FORBIDDEN, "error_missing_perm", rootUuid);

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		final int initialGroupCount = groups().size();
		final String extraGroupName = "no_perm_group";
		final int nGroups = 21;

		try (Tx tx = tx()) {
			GroupRoot root = meshRoot().getGroupRoot();
			// Create and save some groups
			root.create(extraGroupName, user());
			for (int i = 0; i < nGroups; i++) {
				Group group = root.create("group_" + i, user());
				role().grantPermissions(group, READ_PERM);
			}
			tx.success();
		}

		int totalGroups = 0;
		totalGroups = nGroups + data().getGroups().size();
		// Test default paging parameters
		GroupListResponse listResponse = call(() -> client().findGroups());
		assertEquals(25, listResponse.getMetainfo().getPerPage());
		assertEquals(1, listResponse.getMetainfo().getCurrentPage());
		assertEquals(initialGroupCount + nGroups, listResponse.getData().size());

		int perPage = 6;
		listResponse = call(() -> client().findGroups(new PagingParametersImpl(3, perPage)));

		assertEquals(perPage, listResponse.getData().size());

		// created groups + test data group
		int totalPages = (int) Math.ceil(totalGroups / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, listResponse.getData().size());
		assertEquals(3, listResponse.getMetainfo().getCurrentPage());
		assertEquals("We expect {" + totalGroups + "} groups and with a paging size of {" + perPage + "} exactly {" + totalPages + "} pages.",
				totalPages, listResponse.getMetainfo().getPageCount());
		assertEquals(perPage, listResponse.getMetainfo().getPerPage());
		assertEquals(totalGroups, listResponse.getMetainfo().getTotalCount());

		List<GroupResponse> allGroups = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			final int currentPage = page;
			listResponse = call(() -> client().findGroups(new PagingParametersImpl(currentPage, perPage)));
			allGroups.addAll(listResponse.getData());
		}
		assertEquals("Somehow not all groups were loaded when loading all pages.", totalGroups, allGroups.size());

		// Verify that extra group is not part of the response
		List<GroupResponse> filteredUserList = allGroups.parallelStream().filter(restGroup -> restGroup.getName().equals(extraGroupName))
				.collect(Collectors.toList());
		assertTrue("Extra group should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		call(() -> client().findGroups(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

		call(() -> client().findGroups(new PagingParametersImpl(1, -1)), BAD_REQUEST, "error_pagesize_parameter", "-1");

		GroupListResponse response = call(() -> client().findGroups(new PagingParametersImpl(4242, 1)));

		assertEquals(0, response.getData().size());
		assertEquals(4242, response.getMetainfo().getCurrentPage());
		assertEquals(nGroups + initialGroupCount, response.getMetainfo().getPageCount());
		assertEquals(nGroups + initialGroupCount, response.getMetainfo().getTotalCount());
		assertEquals(1, response.getMetainfo().getPerPage());
	}

	@Test
	public void testReadMetaCountOnly() {
		GroupListResponse list = call(() -> client().findGroups(new PagingParametersImpl(1, 0)));
		assertEquals(0, list.getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		GroupResponse response = call(() -> client().findGroupByUuid(groupUuid()));
		try (Tx tx = tx()) {
			assertThat(response).matches(group());
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		GroupResponse response = call(() -> client().findGroupByUuid(groupUuid(), new RolePermissionParametersImpl().setRoleUuid(roleUuid())));
		assertNotNull(response.getRolePerms());
		assertThat(response.getRolePerms()).hasPerm(READ, READ_PUBLISHED, PUBLISH, UPDATE, DELETE, CREATE);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			Group group = group();
			role().revokePermissions(group, READ_PERM);
			assertNotNull("The UUID of the group must not be null.", group.getUuid());
			tx.success();
		}
		call(() -> client().findGroupByUuid(groupUuid()), FORBIDDEN, "error_missing_perm", groupUuid());
	}

	@Test
	public void testReadGroupWithBogusUUID() throws Exception {
		final String bogusUuid = "sadgasdasdg";
		call(() -> client().findGroupByUuid(bogusUuid), NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String name = "New Name";
		String groupUuid = groupUuid();

		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		GroupResponse restGroup = call(() -> client().updateGroup(groupUuid, request));
		assertThat(trackingSearchProvider()).hasStore(Group.composeIndexName(), groupUuid);
		assertThat(trackingSearchProvider()).hasStore(User.composeIndexName(), userUuid());
		assertThat(trackingSearchProvider()).hasEvents(2, 0, 0, 0);

		try (Tx tx = tx()) {
			assertThat(restGroup).matches(request);
			Group reloadedGroup = boot().groupRoot().findByUuid(groupUuid);
			assertEquals("The group should have been updated", name, reloadedGroup.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(group(), UPDATE_PERM);
			tx.success();
		}

		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("new Name");
		call(() -> client().updateGroup(groupUuid(), request), FORBIDDEN, "error_missing_perm", groupUuid());
	}

	@Test
	public void testUpdateGroupWithEmptyName() throws GenericRestException, Exception {
		final String name = "";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);

		String oldName;
		try (Tx tx = tx()) {
			Group group = group();
			oldName = group.getName();
			role().grantPermissions(group, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().updateGroup(groupUuid(), request), BAD_REQUEST, "error_name_must_be_set");

		try (Tx tx = tx()) {
			Group reloadedGroup = boot().groupRoot().findByUuid(groupUuid());
			assertEquals("The group should not have been updated", oldName, reloadedGroup.getName());
		}
	}

	@Test
	public void testUpdateGroupWithConflictingName() throws GenericRestException, Exception {
		final String alreadyUsedName = "extraGroup";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(alreadyUsedName);

		try (Tx tx = tx()) {
			GroupRoot groupRoot = meshRoot().getGroupRoot();
			// Create a group which occupies the name
			assertNotNull(groupRoot.create(alreadyUsedName, user()));
			role().grantPermissions(group(), UPDATE_PERM);
			tx.success();
		}

		call(() -> client().updateGroup(groupUuid(), request), CONFLICT, "group_conflicting_name", alreadyUsedName);

		try (Tx tx = tx()) {
			GroupRoot groupRoot = meshRoot().getGroupRoot();
			Group reloadedGroup = groupRoot.findByUuid(group().getUuid());
			assertEquals("The group should not have been updated", group().getName(), reloadedGroup.getName());
		}

	}

	/**
	 * Tests getting all groups with permissions of a role and checks if every group has the rolePerms set.
	 */
	@Test
	public void testReadWithRolePermsSync() throws Exception {
		try (Tx tx = tx()) {
			// Create a lot of groups
			int groupCount = 100;
			GroupCreateRequest createReq = new GroupCreateRequest();
			for (int i = 0; i < groupCount; i++) {
				createReq.setName("testGroup" + i);
				call(() -> client().createGroup(createReq));
			}

			ParameterProvider[] params = new ParameterProvider[] { new PagingParametersImpl().setPerPage(10000),
					new RolePermissionParametersImpl().setRoleUuid(role().getUuid()) };

			int readCount = 100;
			for (int i = 0; i < readCount; i++) {
				GroupListResponse res = call(() -> client().findGroups(params));
				for (GroupResponse grp : res.getData()) {
					String msg = String.format("Role perms was null after try %d at %s (%s)", i + 1, grp.getName(), grp.getUuid());
					assertNotNull(msg, grp.getRolePerms());
				}
			}
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		call(() -> client().updateGroup("bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {

		call(() -> client().deleteGroup(groupUuid()));
		assertThat(trackingSearchProvider()).hasDelete(Group.composeIndexName(), groupUuid());
		assertThat(trackingSearchProvider()).hasStore(User.composeIndexName(), userUuid());
		assertThat(trackingSearchProvider()).hasEvents(1, 1, 0, 0);

		try (Tx tx = tx()) {
			assertElement(boot().groupRoot(), groupUuid(), false);
		}
	}

	/**
	 * Assert that the shortcut edge between the user and the role is not removed if the user belongs to another group which would grant that role.
	 */
	@Test
	public void testDeleteCase2() {

		// Add second group and add role to that group
		GroupResponse group2 = call(() -> client().createGroup(new GroupCreateRequest().setName("group2")));
		String groupUuid = group2.getUuid();
		call(() -> client().addRoleToGroup(groupUuid, roleUuid()));
		call(() -> client().addUserToGroup(groupUuid, userUuid()));

		// Now delete the first group and let the consistency check do the rest.
		call(() -> client().deleteGroup(groupUuid()));
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		// Don't allow delete
		try (Tx tx = tx()) {
			role().revokePermissions(group(), DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().deleteGroup(groupUuid()), FORBIDDEN, "error_missing_perm", groupUuid());
			assertElement(boot().groupRoot(), groupUuid(), true);
		}
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws InterruptedException {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("changed");

		int nJobs = 5;
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().updateGroup(group().getUuid(), request).invoke());
		}
		validateSet(set, barrier);

	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testReadByUuidMultithreaded() throws InterruptedException {
		int nJobs = 10;
		String uuid = user().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			log.debug("Invoking findGroupByUuid REST call");
			set.add(client().findGroupByUuid(uuid).invoke());
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 10;
		String uuid = db().tx(() -> group().getUuid());
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			log.debug("Invoking deleteUser REST call");
			set.add(client().deleteGroup(uuid).invoke());
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 50;
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			log.debug("Invoking createGroup REST call");
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName("test12345_" + i);
			set.add(client().createGroup(request).invoke());
		}
		validateCreation(set, barrier);

	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		Set<MeshResponse<GroupResponse>> set = new HashSet<>();
		try (Tx tx = tx()) {
			int nJobs = 200;
			for (int i = 0; i < nJobs; i++) {
				log.debug("Invoking findGroupByUuid REST call");
				set.add(client().findGroupByUuid(group().getUuid()).invoke());
			}
		}
		for (MeshResponse<GroupResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

}

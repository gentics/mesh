package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GroupEndpointTest extends AbstractBasicCrudEndpointTest {

	private static final Logger log = LoggerFactory.getLogger(GroupEndpointTest.class);

	@Test
	@Override
	public void testCreate() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("test12345");

		GroupResponse restGroup = call(() -> client().createGroup(request));
		assertThat(restGroup).matches(request);

		assertThat(dummySearchProvider).hasStore(Group.composeIndexName(), Group.composeIndexType(), restGroup.getUuid());
		assertThat(dummySearchProvider).events(1, 0, 0, 0);
		dummySearchProvider.clear();

		try (NoTx noTx = db.noTx()) {
			assertElement(boot.groupRoot(), restGroup.getUuid(), true);
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("test12345");
		String groupRootUuid = db.noTx(() -> meshRoot().getGroupRoot().getUuid());

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
		}

		call(() -> client().createGroup(request), FORBIDDEN, "error_missing_perm", groupRootUuid);
	}

	@Test
	public void testBatchCreation() {
		try (NoTx noTx = db.noTx()) {
			for (int i = 0; i < 10; i++) {
				System.out.println(i);
				final String name = "test_" + i;
				GroupCreateRequest request = new GroupCreateRequest();
				request.setName(name);
				GroupRoot root = meshRoot().getGroupRoot();
				root.reload();
				role().grantPermissions(root, CREATE_PERM);

				GroupResponse restGroup = call(() -> client().createGroup(request));
				assertThat(restGroup).matches(request);
			}
		}
	}

	@Test
	public void testConflicingGroupCreation() throws Exception {
		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);

		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(meshRoot().getGroupRoot(), CREATE_PERM);
			MeshResponse<GroupResponse> future = client().createGroup(request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			assertThat(restGroup).matches(request);

			assertElement(boot.groupRoot(), restGroup.getUuid(), true);
			future = client().createGroup(request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "group_conflicting_name", name);
		}
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// Create the group
			final String name = "test12345";
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);

			MeshResponse<GroupResponse> future = client().createGroup(request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			assertThat(restGroup).matches(request);

			Group foundGroup = boot.groupRoot().findByUuid(restGroup.getUuid());
			assertNotNull("Group should have been created.", foundGroup);

			MeshResponse<GroupResponse> readFuture = client().findGroupByUuid(restGroup.getUuid()).invoke();
			latchFor(readFuture);
			assertSuccess(readFuture);

			// Now delete the group
			MeshResponse<Void> deleteFuture = client().deleteGroup(restGroup.getUuid()).invoke();
			latchFor(deleteFuture);
			assertSuccess(deleteFuture);
		}
	}

	@Test
	public void testCreateGroupWithMissingName() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(group(), CREATE_PERM);
		}
		GroupCreateRequest request = new GroupCreateRequest();
		MeshResponse<GroupResponse> future = client().createGroup(request).invoke();
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_name_must_be_set");

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			final String name = "test12345";
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);
			GroupRoot root = meshRoot().getGroupRoot();
			String rootUuid = root.getUuid();
			role().revokePermissions(root, CREATE_PERM);
			User user = user();
			assertFalse("The create permission to the groups root node should have been revoked.", user.hasPermission(root, CREATE_PERM));

			MeshResponse<GroupResponse> future = client().createGroup(request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", rootUuid);
		}

	}

	// Read Tests

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
			int totalGroups = 0;
			String extraGroupName = "no_perm_group";
			GroupRoot root = meshRoot().getGroupRoot();
			// Create and save some groups
			final int nGroups = 21;
			root.create(extraGroupName, user());
			for (int i = 0; i < nGroups; i++) {
				Group group = root.create("group_" + i, user());
				role().grantPermissions(group, READ_PERM);
			}

			totalGroups = nGroups + groups().size();
			// Test default paging parameters
			MeshResponse<GroupListResponse> future = client().findGroups().invoke();
			latchFor(future);
			assertSuccess(future);
			GroupListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 6;
			future = client().findGroups(new PagingParametersImpl(3, perPage)).invoke();
			latchFor(future);
			assertSuccess(future);
			restResponse = future.result();

			assertEquals(perPage, restResponse.getData().size());

			// created groups + test data group
			int totalPages = (int) Math.ceil(totalGroups / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
			assertEquals(3, restResponse.getMetainfo().getCurrentPage());
			assertEquals("We expect {" + totalGroups + "} groups and with a paging size of {" + perPage + "} exactly {" + totalPages + "} pages.",
					totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals(totalGroups, restResponse.getMetainfo().getTotalCount());

			List<GroupResponse> allGroups = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				MeshResponse<GroupListResponse> pageFuture = client().findGroups(new PagingParametersImpl(page, perPage)).invoke();
				latchFor(pageFuture);
				assertSuccess(pageFuture);
				restResponse = pageFuture.result();
				allGroups.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all groups were loaded when loading all pages.", totalGroups, allGroups.size());

			// Verify that extra group is not part of the response
			List<GroupResponse> filteredUserList = allGroups.parallelStream().filter(restGroup -> restGroup.getName().equals(extraGroupName))
					.collect(Collectors.toList());
			assertTrue("Extra group should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			call(() -> client().findGroups(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = client().findGroups(new PagingParametersImpl(1, -1)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = client().findGroups(new PagingParametersImpl(4242, 1)).invoke();
			latchFor(future);
			assertSuccess(future);

			assertEquals(0, future.result().getData().size());
			assertEquals(4242, future.result().getMetainfo().getCurrentPage());
			assertEquals(25, future.result().getMetainfo().getPageCount());
			assertEquals(25, future.result().getMetainfo().getTotalCount());
			assertEquals(1, future.result().getMetainfo().getPerPage());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		MeshResponse<GroupListResponse> future = client().findGroups(new PagingParametersImpl(1, 0)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			assertNotNull("The UUID of the group must not be null.", group.getUuid());

			MeshResponse<GroupResponse> future = client().findGroupByUuid(group.getUuid()).invoke();
			latchFor(future);
			assertSuccess(future);
			assertThat(future.result()).matches(group());
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			String uuid = group.getUuid();

			MeshResponse<GroupResponse> future = client().findGroupByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid()))
					.invoke();
			latchFor(future);
			assertSuccess(future);
			assertNotNull(future.result().getRolePerms());
			assertThat(future.result().getRolePerms()).containsOnly("read", "readpublished", "publish", "update", "delete", "create");
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			role().revokePermissions(group, READ_PERM);
			assertNotNull("The UUID of the group must not be null.", group.getUuid());
			MeshResponse<GroupResponse> future = client().findGroupByUuid(group.getUuid()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
		}
	}

	@Test
	public void testReadGroupWithBogusUUID() throws Exception {
		final String bogusUuid = "sadgasdasdg";
		MeshResponse<GroupResponse> future = client().findGroupByUuid(bogusUuid).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String name = "New Name";
		String uuid = db.noTx(() -> group().getUuid());
		String userUuid = db.noTx(() -> user().getUuid());

		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		GroupResponse restGroup = call(() -> client().updateGroup(uuid, request));
		assertThat(dummySearchProvider).hasStore(Group.composeIndexName(), Group.composeIndexType(), uuid);
		assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), userUuid);
		assertThat(dummySearchProvider).events(2, 0, 0, 0);

		try (Tx tx = db.tx()) {
			assertThat(restGroup).matches(request);
			Group reloadedGroup = boot.groupRoot().findByUuid(uuid);
			assertEquals("The group should have been updated", name, reloadedGroup.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(group(), UPDATE_PERM);
		}
		String uuid = db.noTx(() -> group().getUuid());
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("new Name");
		call(() -> client().updateGroup(uuid, request), FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testUpdateGroupWithEmptyName() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {

			Group group = group();

			role().grantPermissions(group, UPDATE_PERM);
			final String name = "";
			GroupUpdateRequest request = new GroupUpdateRequest();
			request.setName(name);

			MeshResponse<GroupResponse> future = client().updateGroup(group.getUuid(), request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_name_must_be_set");

			Group reloadedGroup = boot.groupRoot().findByUuid(group.getUuid());
			assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
		}
	}

	@Test
	public void testUpdateGroupWithConflictingName() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {
			final String alreadyUsedName = "extraGroup";
			GroupRoot groupRoot = meshRoot().getGroupRoot();
			// Create a group which occupies the name
			assertNotNull(groupRoot.create(alreadyUsedName, user()));
			role().grantPermissions(group(), UPDATE_PERM);
			GroupUpdateRequest request = new GroupUpdateRequest();
			request.setName(alreadyUsedName);

			MeshResponse<GroupResponse> future = client().updateGroup(group().getUuid(), request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "group_conflicting_name", alreadyUsedName);

			Group reloadedGroup = groupRoot.findByUuid(group().getUuid());
			assertEquals("The group should not have been updated", group().getName(), reloadedGroup.getName());
		}
	}

	/**
	 * Tests getting all groups with permissions of a role and checks if every group has the rolePerms set.
	 */
	@Test
	public void testReadWithRolePermsSync() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// Create a lot of groups
			int groupCount = 100;
			GroupCreateRequest createReq = new GroupCreateRequest();
			for (int i = 0; i < groupCount; i++) {
				createReq.setName("testGroup" + i);
				MeshResponse<GroupResponse> future = client().createGroup(createReq).invoke();
				latchFor(future);
			}

			ParameterProvider[] params = new ParameterProvider[] { new PagingParametersImpl().setPerPage(10000),
					new RolePermissionParameters().setRoleUuid(role().getUuid()) };

			int readCount = 100;
			for (int i = 0; i < readCount; i++) {
				MeshResponse<GroupListResponse> fut = client().findGroups(params).invoke();
				latchFor(fut);
				GroupListResponse res = fut.result();

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
		call(() -> client().updateGroup("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String groupUuid = db.noTx(() -> group().getUuid());
		String userUuid = db.noTx(() -> user().getUuid());

		call(() -> client().deleteGroup(groupUuid));
		assertThat(dummySearchProvider).hasDelete(Group.composeIndexName(), Group.composeIndexType(), groupUuid);
		assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), userUuid);
		assertThat(dummySearchProvider).events(1, 1, 0, 0);

		try (NoTx noTx = db.noTx()) {
			assertElement(boot.groupRoot(), groupUuid, false);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			String uuid = group.getUuid();
			assertNotNull(uuid);
			// Don't allow delete
			role().revokePermissions(group, DELETE_PERM);

			call(() -> client().deleteGroup(uuid), FORBIDDEN, "error_missing_perm", group.getUuid());
			assertElement(boot.groupRoot(), group.getUuid(), true);
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
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 3;
		String uuid = group().getUuid();
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
	@Ignore("not yet enabled")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
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
		try (NoTx noTx = db.noTx()) {
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

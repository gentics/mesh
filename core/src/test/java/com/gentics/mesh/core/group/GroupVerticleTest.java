package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
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

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicIsolatedCrudVerticleTest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GroupVerticleTest extends AbstractBasicIsolatedCrudVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(GroupVerticleTest.class);

	private GroupVerticle verticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	// Create Tests
	@Test
	@Override
	public void testCreate() throws Exception {
		final String name = "test12345";
		try (NoTx noTx = db.noTx()) {
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);
			role().grantPermissions(meshRoot().getGroupRoot(), CREATE_PERM);

			MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(request, restGroup);
			assertElement(boot.groupRoot(), restGroup.getUuid(), true);
		}
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

				MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
				latchFor(future);
				assertSuccess(future);
				GroupResponse restGroup = future.result();
				test.assertGroup(request, restGroup);
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
			MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(request, restGroup);

			assertElement(boot.groupRoot(), restGroup.getUuid(), true);
			future = getClient().createGroup(request).invoke();
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

			MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(request, restGroup);

			Group foundGroup = boot.groupRoot().findByUuid(restGroup.getUuid()).toBlocking().value();
			assertNotNull("Group should have been created.", foundGroup);

			MeshResponse<GroupResponse> readFuture = getClient().findGroupByUuid(restGroup.getUuid()).invoke();
			latchFor(readFuture);
			assertSuccess(readFuture);

			// Now delete the group
			MeshResponse<Void> deleteFuture = getClient().deleteGroup(restGroup.getUuid()).invoke();
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
		MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_name_must_be_set");

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			final String name = "test12345";
			GroupCreateRequest request = new GroupCreateRequest();
			InternalActionContext ac = getMockedInternalActionContext();
			request.setName(name);
			String rootUuid;
			GroupRoot root = meshRoot().getGroupRoot();
			rootUuid = root.getUuid();
			role().revokePermissions(root, CREATE_PERM);
			User user = user();
			assertFalse("The create permission to the groups root node should have been revoked.",
					user.hasPermissionAsync(ac, root, CREATE_PERM).toBlocking().value());

			MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
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
			MeshResponse<GroupListResponse> future = getClient().findGroups().invoke();
			latchFor(future);
			assertSuccess(future);
			GroupListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 6;
			future = getClient().findGroups(new PagingParameters(3, perPage)).invoke();
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
				MeshResponse<GroupListResponse> pageFuture = getClient().findGroups(new PagingParameters(page, perPage)).invoke();
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

			future = getClient().findGroups(new PagingParameters(-1, perPage)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = getClient().findGroups(new PagingParameters(1, -1)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = getClient().findGroups(new PagingParameters(4242, 1)).invoke();
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
		MeshResponse<GroupListResponse> future = getClient().findGroups(new PagingParameters(1, 0)).invoke();
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

			MeshResponse<GroupResponse> future = getClient().findGroupByUuid(group.getUuid()).invoke();
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

			MeshResponse<GroupResponse> future = getClient().findGroupByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())).invoke();
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
			MeshResponse<GroupResponse> future = getClient().findGroupByUuid(group.getUuid()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
		}
	}

	@Test
	public void testReadGroupWithBogusUUID() throws Exception {
		final String bogusUuid = "sadgasdasdg";
		MeshResponse<GroupResponse> future = getClient().findGroupByUuid(bogusUuid).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	// Update Tests

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);

		GroupResponse updatedGroup = db.noTx(() -> {
			Group group = group();

			MeshResponse<GroupResponse> future = getClient().updateGroup(group.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(request, restGroup);
			return restGroup;
		});
		try (Tx tx = db.tx()) {
			Group reloadedGroup = boot.groupRoot().findByUuid(updatedGroup.getUuid()).toBlocking().value();
			assertEquals("The group should have been updated", name, reloadedGroup.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(group(), UPDATE_PERM);
			String uuid = group().getUuid();
			GroupUpdateRequest request = new GroupUpdateRequest();
			request.setName("new Name");

			MeshResponse<GroupResponse> future = getClient().updateGroup(uuid, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	public void testUpdateGroupWithEmptyName() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {

			Group group = group();

			role().grantPermissions(group, UPDATE_PERM);
			final String name = "";
			GroupUpdateRequest request = new GroupUpdateRequest();
			request.setName(name);

			MeshResponse<GroupResponse> future = getClient().updateGroup(group.getUuid(), request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_name_must_be_set");

			Group reloadedGroup = boot.groupRoot().findByUuid(group.getUuid()).toBlocking().value();
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

			MeshResponse<GroupResponse> future = getClient().updateGroup(group().getUuid(), request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "group_conflicting_name", alreadyUsedName);

			Group reloadedGroup = groupRoot.findByUuid(group().getUuid()).toBlocking().value();
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
				MeshResponse<GroupResponse> future = getClient().createGroup(createReq).invoke();
				latchFor(future);
			}

			ParameterProvider[] params = new ParameterProvider[] { new PagingParameters().setPerPage(10000),
					new RolePermissionParameters().setRoleUuid(role().getUuid()) };

			int readCount = 100;
			for (int i = 0; i < readCount; i++) {
				MeshResponse<GroupListResponse> fut = getClient().findGroups(params).invoke();
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

		MeshResponse<GroupResponse> future = getClient().updateGroup("bogus", request).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	// Delete Tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			String name = group.getName();
			String uuid = group.getUuid();
			assertNotNull(uuid);
			MeshResponse<Void> future = getClient().deleteGroup(uuid).invoke();
			latchFor(future);
			assertSuccess(future);
			assertElement(boot.groupRoot(), uuid, false);
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

			MeshResponse<Void> future = getClient().deleteGroup(uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
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
			set.add(getClient().updateGroup(group().getUuid(), request).invoke());
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
			set.add(getClient().findGroupByUuid(uuid).invoke());
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
			set.add(getClient().deleteGroup(uuid).invoke());
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
			set.add(getClient().createGroup(request).invoke());
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
				set.add(getClient().findGroupByUuid(group().getUuid()).invoke());
			}
		}
		for (MeshResponse<GroupResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

}

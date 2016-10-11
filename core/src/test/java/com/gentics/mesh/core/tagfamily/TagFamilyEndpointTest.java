package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

public class TagFamilyEndpointTest extends AbstractBasicCrudEndpointTest {

	@Test
	@Override
	public void testReadByUUID() throws UnknownHostException, InterruptedException {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
			assertNotNull(tagFamily);

			MeshResponse<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid()).invoke();
			latchFor(future);
			assertSuccess(future);
			TagFamilyResponse response = future.result();

			assertNotNull(response);
			assertEquals(tagFamily.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
			String uuid = tagFamily.getUuid();

			MeshResponse<TagFamilyResponse> future = getClient()
					.findTagFamilyByUuid(PROJECT_NAME, uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())).invoke();
			latchFor(future);
			assertSuccess(future);
			assertNotNull("The response did not contain the expected role permission field value", future.result().getRolePerms());
			assertEquals("The response did not contain the expected amount of role permissions.", 6, future.result().getRolePerms().length);
		}

	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
			String uuid = tagFamily.getUuid();
			assertNotNull(tagFamily);
			role.revokePermissions(tagFamily, READ_PERM);

			MeshResponse<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	public void testReadMultiple2() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("colors");
			String uuid = tagFamily.getUuid();
			MeshResponse<TagListResponse> future = getClient().findTags(PROJECT_NAME, uuid).invoke();
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws UnknownHostException, InterruptedException {
		try (NoTx noTx = db.noTx()) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily noPermTagFamily = project().getTagFamilyRoot().create("noPermTagFamily", user());
			String noPermTagUUID = noPermTagFamily.getUuid();
			// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
			// noPermTag.addProject(project());
			assertNotNull(noPermTagFamily.getUuid());

			// Test default paging parameters
			MeshResponse<TagFamilyListResponse> future = getClient().findTagFamilies(PROJECT_NAME).invoke();
			latchFor(future);
			assertSuccess(future);

			TagFamilyListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The response did not contain the correct amount of items", tagFamilies().size(), restResponse.getData().size());

			int perPage = 4;
			// Extra Tags + permitted tag
			int totalTagFamilies = tagFamilies().size();
			int totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
			List<TagFamilyResponse> allTagFamilies = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				MeshResponse<TagFamilyListResponse> tagPageFut = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(page, perPage))
						.invoke();
				latchFor(tagPageFut);
				assertSuccess(future);
				restResponse = tagPageFut.result();
				int expectedItemsCount = perPage;
				// Check the last page
				if (page == 1) {
					expectedItemsCount = 2;
				}
				assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
				assertEquals(perPage, restResponse.getMetainfo().getPerPage());
				assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page,
						restResponse.getMetainfo().getCurrentPage());
				assertEquals("The amount of total pages did not match the expected value. There are {" + totalTagFamilies + "} tags and {" + perPage
						+ "} tags per page", totalPages, restResponse.getMetainfo().getPageCount());
				assertEquals("The total tag count does not match.", totalTagFamilies, restResponse.getMetainfo().getTotalCount());

				allTagFamilies.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all users were loaded when loading all pages.", totalTagFamilies, allTagFamilies.size());

			// Verify that the no_perm_tag is not part of the response
			List<TagFamilyResponse> filteredUserList = allTagFamilies.parallelStream().filter(restTag -> restTag.getUuid().equals(noPermTagUUID))
					.collect(Collectors.toList());
			assertTrue("The no perm tag should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			MeshResponse<TagFamilyListResponse> pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(-1, perPage)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(0, perPage)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "0");

			pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(1, -1)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_pagesize_parameter", "-1");

			perPage = 25;
			totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
			pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(4242, perPage)).invoke();
			latchFor(pageFuture);
			TagFamilyListResponse tagList = pageFuture.result();
			assertEquals(0, tagList.getData().size());
			assertEquals(4242, tagList.getMetainfo().getCurrentPage());
			assertEquals(25, tagList.getMetainfo().getPerPage());
			assertEquals(totalTagFamilies, tagList.getMetainfo().getTotalCount());
			assertEquals(totalPages, tagList.getMetainfo().getPageCount());
		}

	}

	@Test
	public void testReadMetaCountOnly() {
		MeshResponse<TagFamilyListResponse> pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameters(1, 0)).invoke();
		latchFor(pageFuture);
		assertSuccess(pageFuture);
		assertEquals(0, pageFuture.result().getData().size());
	}

	@Test
	public void testCreateWithConflictingName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("colors");
		call(() -> getClient().createTagFamily(PROJECT_NAME, request), CONFLICT, "tagfamily_conflicting_name", "colors");
	}

	@Test
	@Override
	public void testCreate() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");
		TagFamilyResponse response = call(() -> getClient().createTagFamily(PROJECT_NAME, request));
		assertEquals(request.getName(), response.getName());
	}

	@Test
	@Override
	public void testCreateWithNoPerm() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");
		String tagFamilyRootUuid = db.noTx(() -> project().getTagFamilyRoot().getUuid());
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
		}
		call(() -> getClient().createTagFamily(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", tagFamilyRootUuid);
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");

		// 1. Create
		TagFamilyResponse tagFamily = call(() -> getClient().createTagFamily(PROJECT_NAME, request));

		// 2. Read
		String tagFamilyUuid = tagFamily.getUuid();
		tagFamily = call(() -> getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamilyUuid));

		// 3. Delete
		String tagFamilyUuid2 = tagFamily.getUuid();
		call(() -> getClient().deleteTagFamily(PROJECT_NAME, tagFamilyUuid2));

	}

	@Test
	public void testCreateWithoutPerm() {
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
			TagFamilyCreateRequest request = new TagFamilyCreateRequest();
			request.setName("SuperDoll");
			MeshResponse<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request).invoke();
			latchFor(future);

			expectException(future, FORBIDDEN, "error_missing_perm", project().getTagFamilyRoot().getUuid());
		}
	}

	@Test
	public void testCreateWithNoName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		// Don't set the name
		MeshResponse<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request).invoke();
		latchFor(future);
		expectException(future, BAD_REQUEST, "tagfamily_name_not_set");
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			TagFamily basicTagFamily = tagFamily("basic");
			String uuid = basicTagFamily.getUuid();
			assertNotNull(project().getTagFamilyRoot().findByUuid(uuid));

			call(() -> getClient().deleteTagFamily(PROJECT_NAME, uuid));
			assertElement(project().getTagFamilyRoot(), uuid, false);
		}

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			TagFamily basicTagFamily = tagFamily("basic");
			Role role = role();
			role.revokePermissions(basicTagFamily, DELETE_PERM);

			assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);

			call(() -> getClient().deleteTagFamily(PROJECT_NAME, basicTagFamily.getUuid()), FORBIDDEN, "error_missing_perm",
					basicTagFamily.getUuid());

			assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);
		}

	}

	@Test
	public void testUpdateWithConflictingName() {
		try (NoTx noTx = db.noTx()) {
			String newName = "colors";
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName(newName);

			MeshResponse<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, tagFamily("basic").getUuid(), request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "tagfamily_conflicting_name", newName);
		}
	}

	@Test
	@Override
	public void testUpdate() throws UnknownHostException, InterruptedException {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("basic");
			String uuid = tagFamily.getUuid();
			String name = tagFamily.getName();

			// 1. Read the current tagfamily
			MeshResponse<TagFamilyResponse> readTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid).invoke();
			latchFor(readTagFut);
			assertSuccess(readTagFut);
			assertNotNull("The name of the tag should be loaded.", name);
			String restName = readTagFut.result().getName();
			assertNotNull("The tag name must be set.", restName);
			assertEquals(name, restName);

			// 2. Update the tagfamily
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName("new Name");
			TagFamilyUpdateRequest tagUpdateRequest = new TagFamilyUpdateRequest();
			final String newName = "new Name";
			tagUpdateRequest.setName(newName);
			assertEquals(newName, tagUpdateRequest.getName());

			// 3. Send the request to the server
			MeshResponse<TagFamilyResponse> updatedTagFut = getClient().updateTagFamily(PROJECT_NAME, uuid, tagUpdateRequest).invoke();
			latchFor(updatedTagFut);
			assertSuccess(updatedTagFut);
			TagFamilyResponse tagFamily2 = updatedTagFut.result();
			assertThat(tagFamily2).matches(tagFamily("basic"));

			// 4. read the tag again and verify that it was changed
			MeshResponse<TagFamilyResponse> reloadedTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid).invoke();
			latchFor(reloadedTagFut);
			assertSuccess(reloadedTagFut);
			TagFamilyResponse reloadedTagFamily = reloadedTagFut.result();
			assertEquals(request.getName(), reloadedTagFamily.getName());
			assertThat(reloadedTagFamily).matches(tagFamily("basic"));
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("new Name");

		MeshResponse<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, "bogus", request).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("basic");
			String uuid = tagFamily.getUuid();
			String name = tagFamily.getName();
			role().revokePermissions(tagFamily, UPDATE_PERM);

			// Update the tagfamily
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName("new Name");

			MeshResponse<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, uuid, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			assertEquals(name, tagFamily.getName());
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		int nJobs = 5;
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("New Name");

		try (NoTx noTx = db.noTx()) {
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().updateTagFamily(PROJECT_NAME, tagFamily("colors").getUuid(), request).invoke());
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (NoTx noTx = db.noTx()) {
			String uuid = tagFamily("colors").getUuid();
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findTagFamilyByUuid(PROJECT_NAME, uuid).invoke());
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (NoTx noTx = db.noTx()) {
			String uuid = project().getUuid();
			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<Void>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().deleteTagFamily(PROJECT_NAME, uuid).invoke());
			}
			validateDeletion(set, barrier);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("test12345");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().createTagFamily(PROJECT_NAME, request).invoke());
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 200;
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily("colors").getUuid()).invoke());
			}
			for (MeshResponse<?> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

}

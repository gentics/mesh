package com.gentics.mesh.core.tag;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testReadMultiple() throws Exception {

		final int nBasicTags = 9;
		try (Tx tx = tx()) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily basicTagFamily = tagFamily("basic");
			Tag noPermTag = basicTagFamily.create("noPermTag", project(), user());
			String noPermTagUUID = noPermTag.getUuid();
			// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
			basicTagFamily.addTag(noPermTag);
			assertNotNull(noPermTag.getUuid());

			// Test default paging parameters
			ListResponse<TagResponse> restResponse = call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid()));
			assertNull(restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The response did not contain the correct amount of items. We only have nine basic tags in the test data.", nBasicTags,
					restResponse.getData().size());

			final long perPage = 4;
			// Extra Tags + permitted tag
			int totalTags = nBasicTags;
			int totalPages = (int) Math.ceil(totalTags / (double) perPage);
			List<TagResponse> allTags = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				final int currentPage = page;
				TagListResponse tagPage = call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid(), new PagingParametersImpl(currentPage,
						perPage)));
				restResponse = tagPage;
				long expectedItemsCount = perPage;
				// The last page should only list 5 items
				if (page == 3) {
					expectedItemsCount = 1;
				}
				assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
				assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());
				assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page, restResponse.getMetainfo()
						.getCurrentPage());
				assertEquals("The amount of total pages did not match the expected value. There are {" + totalTags + "} tags and {" + perPage
						+ "} tags per page", totalPages, restResponse.getMetainfo().getPageCount());
				assertEquals("The total tag count does not match.", totalTags, restResponse.getMetainfo().getTotalCount());

				allTags.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all users were loaded when loading all pages.", totalTags, allTags.size());

			// Verify that the no_perm_tag is not part of the response
			List<TagResponse> filteredUserList = allTags.parallelStream().filter(restTag -> restTag.getUuid().equals(noPermTagUUID)).collect(
					Collectors.toList());
			assertTrue("The no perm tag should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid(), new PagingParametersImpl(-1, perPage)), BAD_REQUEST,
					"error_page_parameter_must_be_positive", "-1");

			call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid(), new PagingParametersImpl(0, perPage)), BAD_REQUEST,
					"error_page_parameter_must_be_positive", "0");

			call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid(), new PagingParametersImpl(1, -1L)), BAD_REQUEST,
					"error_pagesize_parameter", "-1");

			long currentPerPage = 25L;
			totalPages = (int) Math.ceil(totalTags / (double) currentPerPage);
			TagListResponse tagList = call(() -> client().findTags(PROJECT_NAME, basicTagFamily.getUuid(), new PagingParametersImpl(4242,
					currentPerPage)));
			assertEquals(0, tagList.getData().size());
			assertEquals(4242, tagList.getMetainfo().getCurrentPage());
			assertEquals(currentPerPage, tagList.getMetainfo().getPerPage().longValue());
			assertEquals(nBasicTags, tagList.getMetainfo().getTotalCount());
			assertEquals(totalPages, tagList.getMetainfo().getPageCount());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			TagListResponse pageResponse = client().findTags(PROJECT_NAME, parentTagFamily.getUuid(), new PagingParametersImpl(1, 0L)).blockingGet();
			assertEquals(0, pageResponse.getData().size());
		}
	}

	@Test
	public void testReadByUUID() throws Exception {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			TagFamily parentTagFamily = tagFamily("colors");

			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			TagResponse response = client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), tag.getUuid()).blockingGet();
			assertThat(response).matches(tag);
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			String uuid = tag.getUuid();
			TagFamily parentTagFamily = tagFamily("colors");

			TagResponse response = call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), uuid, new RolePermissionParametersImpl()
					.setRoleUuid(role().getUuid())));
			assertThat(response.getRolePerms()).as("Role perms").hasPerm(CREATE, READ, UPDATE, DELETE);
		}
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {
		String uuid;
		String parentTagFamilyUuid;
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("basic");
			parentTagFamilyUuid = parentTagFamily.getUuid();
			Tag tag = tag("vehicle");
			uuid = tag.getUuid();
			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			role().revokePermissions(tag, READ_PERM);
			tx.success();
		}

		call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamilyUuid, uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		Tag tag = tag("vehicle");
		String tagUuid = tx(() -> tag.getUuid());

		TagFamily parentTagFamily = tagFamily("basic");
		String parentTagFamilyUuid = tx(() -> parentTagFamily.getUuid());

		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		List<? extends Node> nodes;

		try (Tx tx = tx()) {
			String tagName = tag.getName();
			assertNotNull(tag.getEditor());
			TagResponse restTag = call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), tag.getUuid()));

			// 1. Read the current tag
			assertNotNull("The name of the tag should be loaded.", tagName);
			String restName = restTag.getName();
			assertNotNull("The tag name must be set.", restName);
			assertEquals(tagName, restName);

			// 2. Update the tag
			final String newName = "new Name";
			tagUpdateRequest.setName(newName);
			assertEquals(newName, tagUpdateRequest.getName());

			// 3. Send the request to the server
			trackingSearchProvider().clear().blockingAwait();
			nodes = tag.getNodes(project().getLatestBranch()).list();
			tx.success();
		}

		TagResponse tag2 = call(() -> client().updateTag(PROJECT_NAME, parentTagFamilyUuid, tagUuid, tagUpdateRequest));
		try (Tx tx = tx()) {
			assertThat(tag2).matches(tag);
			assertThat(trackingSearchProvider()).hasStore(Tag.composeIndexName(project().getUuid()), Tag.composeDocumentId(tag2.getUuid()));
			// Assert that all nodes which previously referenced the tag were updated in the index
			String projectUuid = project().getUuid();
			String branchUuid = project().getLatestBranch().getUuid();
			for (Node node : nodes) {
				String schemaContainerVersionUuid = node.getLatestDraftFieldContainer(english()).getSchemaContainerVersion().getUuid();
				for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
					assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid,
							schemaContainerVersionUuid, type), NodeGraphFieldContainer.composeDocumentId(node.getUuid(), "en"));
					assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid,
							schemaContainerVersionUuid, type), NodeGraphFieldContainer.composeDocumentId(node.getUuid(), "de"));
				}
			}
			assertThat(trackingSearchProvider()).hasStore(TagFamily.composeIndexName(projectUuid), TagFamily.composeDocumentId(parentTagFamily
					.getUuid()));
			assertThat(trackingSearchProvider()).hasEvents(2 + (nodes.size() * 4), 0, 0, 0);

			// 4. read the tag again and verify that it was changed
			TagResponse reloadedTag = call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), tagUuid));
			assertEquals(tagUpdateRequest.getName(), reloadedTag.getName());
			assertThat(reloadedTag).matches(tag);
		}
	}

	@Test
	public void testUpdateTagWithConflictingName() {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			TagFamily parentTagFamily = tagFamily("colors");

			String uuid = tag.getUuid();
			String tagFamilyName = tag.getTagFamily().getName();

			final String newName = "green";
			TagUpdateRequest request = new TagUpdateRequest();
			request.setName(newName);
			TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
			tagUpdateRequest.setName(newName);
			assertEquals(newName, tagUpdateRequest.getName());

			call(() -> client().updateTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid, tagUpdateRequest),
				CONFLICT, "tag_create_tag_with_same_name_already_exists", newName, tagFamilyName);
		}
	}

	@Test
	public void testUpdateTagWithNoName() {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			TagFamily parentTagFamily = tagFamily("colors");

			String uuid = tag.getUuid();
			TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();

			call(() -> client().updateTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid, tagUpdateRequest), BAD_REQUEST, "tag_name_not_set");
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		Tag tag = tag("vehicle");
		TagFamily parentTagFamily = tagFamily("basic");

		try (Tx tx = tx()) {
			role().revokePermissions(tag, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String tagName = tag.getName();
			String tagUuid = tag.getUuid();

			// Create an tag update request
			TagUpdateRequest request = new TagUpdateRequest();
			request.setName("new Name");
			call(() -> client().updateTag(PROJECT_NAME, parentTagFamily.getUuid(), tagUuid, request), FORBIDDEN, "error_missing_perm", tagUuid, UPDATE_PERM.getRestPerm().getName());

			// read the tag again and verify that it was not changed
			TagResponse loadedTag = client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), tagUuid).blockingGet();
			assertEquals(tagName, loadedTag.getName());
		}

	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String projectUuid = db().tx(() -> project().getUuid());
		String branchUuid = db().tx(() -> project().getLatestBranch().getUuid());

		try (Tx tx = tx()) {
			Tag tag = tag("vehicle");
			TagFamily parentTagFamily = tagFamily("basic");

			List<? extends Node> nodes = tag.getNodes(project().getLatestBranch()).list();

			String uuid = tag.getUuid();
			call(() -> client().deleteTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid));

			assertThat(trackingSearchProvider()).hasDelete(Tag.composeIndexName(projectUuid), Tag.composeDocumentId(uuid));
			// Assert that all nodes which previously referenced the tag were updated in the index
			for (Node node : nodes) {
				String schemaContainerVersionUuid = node.getLatestDraftFieldContainer(english()).getSchemaContainerVersion().getUuid();
				assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid,
						schemaContainerVersionUuid, ContainerType.DRAFT), NodeGraphFieldContainer.composeDocumentId(node.getUuid(), "en"));
			}
			assertThat(trackingSearchProvider()).hasEvents(4, 1, 0, 0);

			tag = boot().tagRoot().findByUuid(uuid);
			assertNull("The tag should have been deleted", tag);

			Project project = boot().projectRoot().findByName(PROJECT_NAME);
			assertNotNull(project);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(tag("vehicle"), DELETE_PERM);
			tx.success();
		}

		String uuid;
		try (Tx tx = tx()) {
			Tag tag = tag("vehicle");
			uuid = tag.getUuid();
			TagFamily parentTagFamily = tagFamily("basic");
			call(() -> client().deleteTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName());
		}

		try (Tx tx = tx()) {
			Tag tag = boot().tagRoot().findByUuid(uuid);
			assertNotNull("The tag should not have been deleted", tag);
		}
	}

	@Test
	public void testCreateConflictingName() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");

			TagCreateRequest tagCreateRequest = new TagCreateRequest();
			tagCreateRequest.setName("red");
			// tagCreateRequest.setTagFamily(new TagFamilyReference().setName(tagFamily.getName()).setUuid(tagFamily.getUuid()));

			MeshRestClientMessageException exception = call(() -> client().createTag(PROJECT_NAME, tagFamily.getUuid(), tagCreateRequest),
				CONFLICT, "tag_create_tag_with_same_name_already_exists", "red", "colors");
			assertNotNull(exception.getResponseMessage().getProperties());
			assertNotNull(exception.getResponseMessage().getProperties().get("conflictingUuid"));
		}
	}

	@Test
	@Override
	public void testCreate() {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setName("SomeName");
		String parentTagFamilyUuid = db().tx(() -> tagFamily("colors").getUuid());
		String projectUuid = db().tx(() -> project().getUuid());

		trackingSearchProvider().clear().blockingAwait();
		TagResponse response = call(() -> client().createTag(PROJECT_NAME, parentTagFamilyUuid, tagCreateRequest));
		assertEquals("SomeName", response.getName());
		assertThat(trackingSearchProvider()).hasStore(Tag.composeIndexName(projectUuid), Tag.composeDocumentId(response.getUuid()));
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0);
		try (Tx tx = tx()) {
			assertNotNull("The tag could not be found within the meshRoot.tagRoot node.", meshRoot().getTagRoot().findByUuid(response.getUuid()));
		}

		String uuid = response.getUuid();
		response = call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamilyUuid, uuid));
		assertEquals("SomeName", response.getName());
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setName("SomeName");
		String parentTagFamilyUuid = db().tx(() -> tagFamily("colors").getUuid());

		String tagRootUuid = db().tx(() -> tagFamily("colors").getUuid());
		try (Tx tx = tx()) {
			role().revokePermissions(tagFamily("colors"), CREATE_PERM);
			tx.success();
		}
		call(() -> client().createTag(PROJECT_NAME, parentTagFamilyUuid, tagCreateRequest), FORBIDDEN, "error_missing_perm", tagRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setName("SomeName");
		String parentTagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String uuid = UUIDUtil.randomUUID();

		TagResponse response = call(() -> client().updateTag(PROJECT_NAME, parentTagFamilyUuid, uuid, tagUpdateRequest));
		assertThat(response).hasName("SomeName").hasUuid(uuid);
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setName("SomeName");
		String parentTagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String uuid = userUuid();

		call(() -> client().updateTag(PROJECT_NAME, parentTagFamilyUuid, uuid, tagUpdateRequest), INTERNAL_SERVER_ERROR, "error_internal");
	}

	@Test
	public void testCreateTagWithSameNameInSameTagFamily() {
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			TagCreateRequest tagCreateRequest = new TagCreateRequest();
			assertNotNull("We expect that a tag with the name already exists.", tag("red"));
			tagCreateRequest.setName("red");
			String tagFamilyName;

			TagFamily tagFamily = data().getTagFamilies().get("colors");
			tagFamilyName = tagFamily.getName();
			// tagCreateRequest.setTagFamily(new TagFamilyReference().setName(tagFamily.getName()).setUuid(tagFamily.getUuid()));
			call(() -> client().createTag(PROJECT_NAME, parentTagFamily.getUuid(), tagCreateRequest), CONFLICT,
					"tag_create_tag_with_same_name_already_exists", "red", tagFamilyName);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		int nJobs = 5;
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			TagUpdateRequest request = new TagUpdateRequest();
			request.setName("newName");
			String uuid = tag("red").getUuid();

			awaitConcurrentRequests(nJobs, i -> client().updateTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid, request));
		}

	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 100;
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			String uuid = tag("red").getUuid();

			awaitConcurrentRequests(nJobs, i -> client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), uuid));
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			String uuid = tag("red").getUuid();
			validateDeletion(i -> client().deleteTag(PROJECT_NAME, parentTagFamily.getUuid(), uuid), nJobs);
		}
	}

	@Test
	@Ignore("Disabled since test is unstable - CL-246")
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 200;
		TagFamily parentTagFamily = tagFamily("colors");

		validateCreation(nJobs, i -> {
			TagCreateRequest request = new TagCreateRequest();
			request.setName("newcolor_" + i);
			return client().createTag(PROJECT_NAME, parentTagFamily.getUuid(), request);
		});
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");

			awaitConcurrentRequests(nJobs, i -> client().findTagByUuid(PROJECT_NAME, parentTagFamily.getUuid(), tag("red").getUuid()));
		}
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setName("SomeName");
		TagFamily tagFamily = data().getTagFamilies().get("colors");
		// tagCreateRequest.setTagFamily(new TagFamilyReference().setName(tagFamily.getName()).setUuid(tagFamily.getUuid()));

		try (Tx tx = tx()) {
			// Create
			TagResponse response = call(() -> client().createTag(PROJECT_NAME, tagFamily.getUuid(), tagCreateRequest));
			assertEquals("SomeName", response.getName());

			// Read
			String uuid = response.getUuid();
			response = call(() -> client().findTagByUuid(PROJECT_NAME, tagFamily.getUuid(), uuid));
			assertEquals("SomeName", response.getName());

			// Delete
			String uuid2 = response.getUuid();
			call(() -> client().deleteTag(PROJECT_NAME, tagFamily.getUuid(), uuid2));
		}

	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		String parentTagFamilyUuid;
		try (Tx tx = tx()) {
			Tag tag = tag("red");

			TagFamily parentTagFamily = tagFamily("colors");
			parentTagFamilyUuid = parentTagFamily.getUuid();

			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			uuid = tag.getUuid();
			role().revokePermissions(tag, READ_PERM);
			tx.success();
		}

		call(() -> client().findTagByUuid(PROJECT_NAME, parentTagFamilyUuid, uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());

	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		TagUpdateRequest request = new TagUpdateRequest();
		request.setName("newName");
		try (Tx tx = tx()) {
			TagFamily parentTagFamily = tagFamily("colors");
			call(() -> client().updateTag(PROJECT_NAME, parentTagFamily.getUuid(), "bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
		}

	}

}

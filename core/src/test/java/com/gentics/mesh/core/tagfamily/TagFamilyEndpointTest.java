package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagFamilyEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testReadByUUID() throws UnknownHostException, InterruptedException {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().iterator().next();
			assertNotNull(tagFamily);
			TagFamilyResponse response = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid()));
			assertNotNull(response);
			assertEquals(tagFamily.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().iterator().next();
			String uuid = tagFamily.getUuid();

			TagFamilyResponse response = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, uuid, new RolePermissionParametersImpl().setRoleUuid(
					role().getUuid())));
			assertNotNull("The response did not contain the expected role permission field value", response.getRolePerms());
			assertThat(response.getRolePerms()).hasPerm(Permission.values());
		}

	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			Role role = role();
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().iterator().next();
			uuid = tagFamily.getUuid();
			assertNotNull(tagFamily);
			role.revokePermissions(tagFamily, READ_PERM);
			tx.success();
		}

		call(() -> client().findTagFamilyByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	@Test
	public void testReadMultiple2() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			String uuid = tagFamily.getUuid();
			call(() -> client().findTags(PROJECT_NAME, uuid));
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws UnknownHostException, InterruptedException {
		try (Tx tx = tx()) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily noPermTagFamily = project().getTagFamilyRoot().create("noPermTagFamily", user());
			String noPermTagUUID = noPermTagFamily.getUuid();
			// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
			// noPermTag.addProject(project());
			assertNotNull(noPermTagFamily.getUuid());

			// Test default paging parameters
			TagFamilyListResponse restResponse = call(() -> client().findTagFamilies(PROJECT_NAME));
			assertNull(restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The response did not contain the correct amount of items", data().getTagFamilies().size(), restResponse.getData().size());

			final long perPage = 4;
			// Extra Tags + permitted tag
			long totalTagFamilies = data().getTagFamilies().size();
			long totalPages = (long) Math.ceil(totalTagFamilies / (double) perPage);
			List<TagFamilyResponse> allTagFamilies = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				final int currentPage = page;
				restResponse = call(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(currentPage, perPage)));
				long expectedItemsCount = perPage;
				// Check the last page
				if (page == 1) {
					expectedItemsCount = 2;
				}
				assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
				assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());
				assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page, restResponse.getMetainfo()
						.getCurrentPage());
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

			call(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			call(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(0, perPage)), BAD_REQUEST,
					"error_page_parameter_must_be_positive", "0");

			call(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(1, -1L)), BAD_REQUEST, "error_pagesize_parameter", "-1");

			long currentPerPage = 25;
			totalPages = (int) Math.ceil(totalTagFamilies / (double) currentPerPage);
			TagFamilyListResponse tagList = call(() -> client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(4242, currentPerPage)));
			assertEquals(0, tagList.getData().size());
			assertEquals(4242, tagList.getMetainfo().getCurrentPage());
			assertEquals(25, tagList.getMetainfo().getPerPage().longValue());
			assertEquals(totalTagFamilies, tagList.getMetainfo().getTotalCount());
			assertEquals(totalPages, tagList.getMetainfo().getPageCount());
		}

	}

	@Test
	public void testReadMetaCountOnly() {
		TagFamilyListResponse page = client().findTagFamilies(PROJECT_NAME, new PagingParametersImpl(1, 0L)).toSingle().blockingGet();
		assertEquals(0, page.getData().size());
	}

	@Test
	public void testCreateWithConflictingName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("colors");
		call(() -> client().createTagFamily(PROJECT_NAME, request), CONFLICT, "tagfamily_conflicting_name", "colors");
	}

	@Test
	@Override
	public void testCreate() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");
		TagFamilyResponse response = call(() -> client().createTagFamily(PROJECT_NAME, request));
		assertEquals(request.getName(), response.getName());
	}

	@Test
	@Override
	public void testCreateWithNoPerm() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");
		String tagFamilyRootUuid = db().tx(() -> project().getTagFamilyRoot().getUuid());
		try (Tx tx = tx()) {
			role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
			tx.success();
		}
		call(() -> client().createTagFamily(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", tagFamilyRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("newTagFamily");
		String uuid = UUIDUtil.randomUUID();
		TagFamilyResponse response = call(() -> client().updateTagFamily(PROJECT_NAME, uuid, request));
		assertThat(response).hasName("newTagFamily").hasUuid(uuid);
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("newTagFamily");
		String uuid = db().tx(() -> user().getUuid());
		call(() -> client().updateTagFamily(PROJECT_NAME, uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");

		// 1. Create
		TagFamilyResponse tagFamily = call(() -> client().createTagFamily(PROJECT_NAME, request));

		// 2. Read
		String tagFamilyUuid = tagFamily.getUuid();
		tagFamily = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamilyUuid));

		// 3. Delete
		String tagFamilyUuid2 = tagFamily.getUuid();
		call(() -> client().deleteTagFamily(PROJECT_NAME, tagFamilyUuid2));

	}

	@Test
	public void testCreateWithoutPerm() {
		try (Tx tx = tx()) {
			role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
			tx.success();
		}
		try (Tx tx = tx()) {
			TagFamilyCreateRequest request = new TagFamilyCreateRequest();
			request.setName("SuperDoll");
			call(() -> client().createTagFamily(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", project().getTagFamilyRoot().getUuid(), CREATE_PERM.getRestPerm().getName());
		}
	}

	@Test
	public void testCreateWithNoName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		// Don't set the name
		call(() -> client().createTagFamily(PROJECT_NAME, request), BAD_REQUEST, "tagfamily_name_not_set");
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (Tx tx = tx()) {
			TagFamily basicTagFamily = tagFamily("basic");
			String uuid = basicTagFamily.getUuid();
			assertNotNull(project().getTagFamilyRoot().findByUuid(uuid));
		}

		String uuid = db().tx(() -> tagFamily("basic").getUuid());
		call(() -> client().deleteTagFamily(PROJECT_NAME, uuid));

		try (Tx tx = tx()) {
			assertElement(project().getTagFamilyRoot(), uuid, false);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		TagFamily basicTagFamily = tagFamily("basic");
		try (Tx tx = tx()) {
			Role role = role();
			role.revokePermissions(basicTagFamily, DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);
			call(() -> client().deleteTagFamily(PROJECT_NAME, basicTagFamily.getUuid()), FORBIDDEN, "error_missing_perm", basicTagFamily.getUuid(), DELETE_PERM.getRestPerm().getName());
			assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);
		}

	}

	@Test
	public void testUpdateWithConflictingName() {
		try (Tx tx = tx()) {
			String newName = "colors";
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName(newName);

			call(() -> client().updateTagFamily(PROJECT_NAME, tagFamily("basic").getUuid(), request), CONFLICT, "tagfamily_conflicting_name",
					newName);
		}
	}

	@Test
	@Override
	public void testUpdate() throws UnknownHostException, InterruptedException {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("basic");
			String uuid = tagFamily.getUuid();
			String name = tagFamily.getName();

			// 1. Read the current tagfamily
			TagFamilyResponse readTagResponse = client().findTagFamilyByUuid(PROJECT_NAME, uuid).blockingGet();
			assertNotNull("The name of the tag should be loaded.", name);
			String restName = readTagResponse.getName();
			assertNotNull("The tag name must be set.", restName);
			assertEquals(name, restName);

			// 2. Update the tagfamily
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName("new Name");

			// 3. Send the request to the server
			TagFamilyResponse tagFamily2 = call(() -> client().updateTagFamily(PROJECT_NAME, uuid, request));
			assertThat(tagFamily2).matches(tagFamily("basic"));

			// 4. read the tag again and verify that it was changed
			TagFamilyResponse reloadedTagFamily = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, uuid));
			assertEquals(request.getName(), reloadedTagFamily.getName());
			assertThat(reloadedTagFamily).matches(tagFamily("basic"));
		}
	}

	@Test
	public void testUpdateNodeIndex() {
		try (Tx tx = tx()) {
			Project project = project();
			Branch branch = project.getBranchRoot().getLatestBranch();
			TagFamily tagfamily = tagFamily("basic");

			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName("basicChanged");
			call(() -> client().updateTagFamily(PROJECT_NAME, tagfamily.getUuid(), request));

			// Multiple tags of the same family can be tagged on same node. This should still trigger only 1 update for that node.
			HashSet<String> taggedNodes = new HashSet<>();
			int storeCount = 0;
			for (Tag tag : tagfamily.findAll()) {
				storeCount++;
				for (Node node : tag.getNodes(branch)) {
					if (!taggedNodes.contains(node.getUuid())) {
						taggedNodes.add(node.getUuid());
						for (ContainerType containerType : new ContainerType[] { ContainerType.DRAFT, ContainerType.PUBLISHED }) {
							for (NodeGraphFieldContainer fieldContainer : node.getGraphFieldContainers(branch, containerType)) {
								SchemaContainerVersion schema = node.getSchemaContainer().getLatestVersion();
								storeCount++;
								assertThat(trackingSearchProvider()).hasStore(NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch
										.getUuid(), schema.getUuid(), containerType), NodeGraphFieldContainer.composeDocumentId(node.getUuid(),
												fieldContainer.getLanguageTag()));
							}
						}
					}
				}
			}

			assertThat(trackingSearchProvider()).hasEvents(storeCount + 1, 0, 0, 0);
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("new Name");

		call(() -> client().updateTagFamily(PROJECT_NAME, "bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() {
		String uuid;
		String name;
		TagFamily tagFamily = tagFamily("basic");
		try (Tx tx = tx()) {
			uuid = tagFamily.getUuid();
			name = tagFamily.getName();
			role().revokePermissions(tagFamily, UPDATE_PERM);
			tx.success();
		}

		// Update the tagfamily
		try (Tx tx = tx()) {
			TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
			request.setName("new Name");
			call(() -> client().updateTagFamily(PROJECT_NAME, uuid, request), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());
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

		try (Tx tx = tx()) {
			awaitConcurrentRequests(i -> client().updateTagFamily(PROJECT_NAME, tagFamily("colors").getUuid(), request), nJobs);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (Tx tx = tx()) {
			String uuid = tagFamily("colors").getUuid();

			awaitConcurrentRequests(i -> client().findTagFamilyByUuid(PROJECT_NAME, uuid), nJobs);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		try (Tx tx = tx()) {
			String uuid = project().getUuid();
			validateDeletion(i -> client().deleteTagFamily(PROJECT_NAME, uuid), nJobs);
		}
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("test12345");

		validateCreation(i -> client().createTagFamily(PROJECT_NAME, request), nJobs);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (Tx tx = tx()) {
			int nJobs = 200;
			awaitConcurrentRequests(i -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamily("colors").getUuid()), nJobs);
		}
	}

}

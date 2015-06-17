package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.rest.group.request.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.request.GroupUpdateRequest;
import com.gentics.mesh.core.rest.group.response.GroupListResponse;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;

public class GroupVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserService userService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	// Create Tests
	@Test
	public void testCreateGroup() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		String requestJson = JsonUtils.toJson(request);

		info.getRole().addPermissions(data().getMeshRoot().getGroupRoot(), CREATE_PERM);

		String response = request(info, POST, "/api/v1/groups/", 200, "OK", requestJson);
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		test.assertGroup(request, restGroup);

		assertNotNull("Group should have been created.", groupService.findByName(name));
	}

	@Test
	public void testCreateDeleteGroup() throws Exception {

		// Create the group
		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/groups/", 200, "OK", requestJson);
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		test.assertGroup(request, restGroup);

		assertNotNull("Group should have been created.", groupService.findByName(name));

		// Now delete the group
		response = request(info, DELETE, "/api/v1/groups/" + restGroup.getUuid(), 200, "OK", requestJson);

		expectMessageResponse("group_deleted", response, restGroup.getUuid());
	}

	@Test
	public void testCreateGroupWithMissingName() throws Exception {

		GroupCreateRequest request = new GroupCreateRequest();

		info.getRole().addPermissions(info.getGroup(), CREATE_PERM);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, POST, "/api/v1/groups/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_name_must_be_set", response);

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		String requestJson = JsonUtils.toJson(request);

		GroupRoot root;
		root = data().getMeshRoot().getGroupRoot();
		info.getRole().revokePermissions(root, CREATE_PERM);
		MeshUser user = info.getUser();
		assertFalse("The create permission to the groups root node should have been revoked.", user.hasPermission(root, CREATE_PERM));

		String response = request(info, POST, "/api/v1/groups/", 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, root.getUuid());

		assertNull(groupService.findByName(name));
	}

	// Read Tests

	@Test
	public void testReadGroups() throws Exception {

		// Create and save some groups
		final int nGroups = 21;
		Group extraGroupWithNoPerm = groupService.create("no_perm_group");
		//		try (Transaction tx = graphDb.beginTx()) {

		for (int i = 0; i < nGroups; i++) {
			Group group = groupService.create("group_" + i);
			info.getRole().addPermissions(group, READ_PERM);
		}
		// Don't grant permissions to extra group
		//			tx.success();
		//		}

		int totalGroups = nGroups + data().getGroups().size();

		// Test default paging parameters
		String response = request(info, GET, "/api/v1/groups/", 200, "OK");
		GroupListResponse restResponse = JsonUtils.readValue(response, GroupListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, GroupListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// created groups + test data group
		int totalPages = (int) Math.ceil(totalGroups / (double) perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals("We expect {" + totalGroups + "} groups and with a paging size of {" + perPage + "} exactly {" + totalPages + "} pages.",
				totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalGroups + 1, restResponse.getMetainfo().getTotalCount());

		List<GroupResponse> allGroups = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			response = request(info, GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, GroupListResponse.class);
			allGroups.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all groups were loaded when loading all pages.", totalGroups + 1, allGroups.size());

		// Verify that extra group is not part of the response
		final String extraGroupName = extraGroupWithNoPerm.getName();
		List<GroupResponse> filteredUserList = allGroups.parallelStream().filter(restGroup -> restGroup.getName().equals(extraGroupName))
				.collect(Collectors.toList());
		assertTrue("Extra group should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, GET, "/api/v1/groups/?per_page=0&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, GET, "/api/v1/groups/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, GET, "/api/v1/groups/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":2,\"total_count\":36}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		Group group = info.getGroup();
		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		test.assertGroup(group, JsonUtils.readValue(response, GroupResponse.class));

	}

	@Test
	public void testReadGroupByUUIDWithNoPermission() throws Exception {
		Group group = info.getGroup();

		info.getRole().revokePermissions(group, READ_PERM);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, GET, "/api/v1/groups/" + group.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, group.getUuid());
	}

	@Test
	public void testReadGroupWithBogusUUID() throws Exception {
		final String bogusUuid = "sadgasdasdg";
		String response = request(info, GET, "/api/v1/groups/" + bogusUuid, 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, bogusUuid);
	}

	// Update Tests

	@Test
	public void testUpdateGroup() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, PUT, "/api/v1/groups/" + group.getUuid(), 200, "OK", JsonUtils.toJson(request));
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		test.assertGroup(request, restGroup);

		//TODO TP load node
		Group reloadedGroup = null;
		Assert.assertEquals("The group should have been updated", name, reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithEmptyName() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		info.getRole().addPermissions(group, UPDATE_PERM);
		final String name = "";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, PUT, "/api/v1/groups/" + group.getUuid(), 400, "Bad Request", JsonUtils.toJson(request));
		expectMessageResponse("error_name_must_be_set", response);

		//TODO TP load node
		Group reloadedGroup = null;
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithConflictingName() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		final String alreadyUsedName = "extraGroup";

		// Create a group which occupies the name
		Group extraGroup = groupService.create(alreadyUsedName);

		info.getRole().addPermissions(group, UPDATE_PERM);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(alreadyUsedName);

		String response = request(info, PUT, "/api/v1/groups/" + group.getUuid(), 400, "Bad Request", JsonUtils.toJson(request));
		expectMessageResponse("group_conflicting_name", response);

		//TODO TP load node
		Group reloadedGroup = null;
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		info.getRole().addPermissions(group, UPDATE_PERM);
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, PUT, "/api/v1/groups/bogus", 404, "Not Found", JsonUtils.toJson(request));
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

		//TODO TP load node
		Group reloadedGroup = null;
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	// Delete Tests

	@Test
	public void testDeleteGroupByUUID() throws Exception {
		Group group = info.getGroup();
		assertNotNull(group.getUuid());

		String response = request(info, DELETE, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		expectMessageResponse("group_deleted", response, group.getUuid());
		assertNull("The group should have been deleted", groupService.findByUUID(group.getUuid()));
	}

	@Test
	public void testDeleteGroupByUUIDWithMissingPermission() throws Exception {
		Group group = info.getGroup();
		assertNotNull(group.getUuid());

		// Don't allow delete
		info.getRole().revokePermissions(group, DELETE_PERM);

		String response = request(info, DELETE, "/api/v1/groups/" + group.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, group.getUuid());
		assertNotNull("The group should not have been deleted", groupService.findByUUID(group.getUuid()));
	}

}

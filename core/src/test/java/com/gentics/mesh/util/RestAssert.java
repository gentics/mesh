package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

@Component
public class RestAssert {

	@Autowired
	private NodeService nodeService;

	@Autowired
	private LanguageService languageService;

	public void assertGroup(Group group, GroupResponse restGroup) {
		assertEquals(group.getUuid(), restGroup.getUuid());
		assertEquals(group.getName(), restGroup.getName());
		// for (User user : group.getUsers()) {
		// assertTrue(restGroup.getUsers().contains(user.getUsername()));
		// }
		// TODO roles
		// group.getRoles()
		// TODO perms
	}

	public void assertGroup(GroupCreateRequest request, GroupResponse restGroup) {
		assertNotNull(request);
		assertNotNull(restGroup);
		assertEquals(request.getName(), restGroup.getName());
		//		assertNotNull(restGroup.getUsers());
		assertNotNull(restGroup.getUuid());
	}

	public void assertUser(User user, UserResponse restUser) {
		assertNotNull("The user must not be null.", user);
		assertNotNull("The restuser must not be null", restUser);
		//		user = neo4jTemplate.fetch(user);
		assertEquals(user.getUsername(), restUser.getUsername());
		assertEquals(user.getEmailAddress(), restUser.getEmailAddress());
		assertEquals(user.getFirstname(), restUser.getFirstname());
		assertEquals(user.getLastname(), restUser.getLastname());
		assertEquals(user.getUuid(), restUser.getUuid());
		assertEquals(user.getGroups().size(), restUser.getGroups().size());
		// TODO groups
	}

	public void assertTag(Tag tag, TagResponse restTag) {
		//		tag.setSchema(neo4jTemplate.fetch(tag.getSchema()));
		assertEquals(tag.getUuid(), restTag.getUuid());
//		assertEquals(tag.getSchema().getUuid(), restTag.getSchema().getUuid());
//		assertEquals(tag.getSchema().getName(), restTag.getSchema().getSchemaName());
	}

	/**
	 * Compare the create request with a content response.
	 * 
	 * @param request
	 * @param restNode
	 */
	public void assertMeshNode(NodeCreateRequest request, NodeResponse restNode) {

//		for (Map.Entry<String, String> entry : request.getProperties().entrySet()) {
//			String value = request.getParentNodeUuid();
//			assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(),
//					restNode.getProperty(entry.getKey()));
//
//		}

		String schemaName = request.getSchema().getName();
		assertEquals("The schemaname of the request does not match the response schema name", schemaName, restNode.getSchema().getName());
//		assertEquals(request.getOrder(), restNode.getOrder());
		String tagUuid = request.getParentNodeUuid();
		// TODO how to match the parent tag?

		assertNotNull(restNode.getUuid());
		assertNotNull(restNode.getCreator());
		assertNotNull(restNode.getPermissions());

	}

	public void assertMeshNode(NodeCreateRequest request, Node node) {
		assertNotNull(request);
		assertNotNull(node);

//		for (Entry<String, String> entry : request.getProperties().entrySet()) {
////			Language language = languageService.findByLanguageTag(languageTag);
//			String propValue = node.getI18nProperties(language).getProperty(entry.getKey());
//			assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(), propValue);
//		}

		assertNotNull(node.getUuid());
		assertNotNull(node.getCreator());
	}

	public void assertMeshNode(Node node, NodeResponse readValue) {
		assertNotNull(node);
		assertNotNull(readValue);
		assertEquals(node.getUuid(), readValue.getUuid());

		//		assertEquals(node.getOrder(), readValue.getOrder());
		assertNotNull(readValue.getPermissions());

		SchemaContainer schema = node.getSchemaContainer();
		assertNotNull("The schema of the test object should not be null. No further assertion can be verified.", schema);
//		assertEquals(schema.getName(), readValue.getSchema().getName());
//		assertEquals(schema.getUuid(), readValue.getSchema().getUuid());

		assertNotNull(readValue.getCreator());

		// TODO match properties

	}

	public void assertRole(Role role, RoleResponse restRole) {
		assertNotNull(role);
		assertNotNull(restRole);
		assertEquals(role.getName(), restRole.getName());
		assertEquals(role.getUuid(), restRole.getUuid());
		assertNotNull(restRole.getPerms());
		assertNotNull(restRole.getGroups());
	}

	public void assertRole(RoleCreateRequest request, RoleResponse restRole) {
		assertNotNull(request);
		assertNotNull(restRole);
		assertEquals(request.getName(), restRole.getName());
		assertNotNull(restRole.getUuid());
		assertNotNull(restRole.getGroups());
	}

	public void assertProject(ProjectCreateRequest request, ProjectResponse restProject) {
		assertNotNull(request);
		assertNotNull(restProject);
		assertEquals(request.getName(), restProject.getName());
		assertNotNull(restProject.getUuid());
		assertNotNull(restProject.getPermissions());
	}

	public void assertProject(Project project, ProjectResponse restProject) {
		assertNotNull(project);
		assertNotNull(restProject);
		assertNotNull(restProject.getUuid());
		assertNotNull(restProject.getPermissions());
		assertNotNull(restProject.getRootNodeUuid());
		assertEquals(project.getName(), restProject.getName());
		assertEquals(project.getUuid(), restProject.getUuid());
	}

	public void assertProject(ProjectUpdateRequest request, ProjectResponse restProject) {
		assertNotNull(request);
		assertNotNull(restProject);
		assertNotNull(restProject.getUuid());
		assertEquals(request.getName(), restProject.getName());
	}

	public void assertSchema(SchemaContainer schema, SchemaResponse restSchema) {
		assertNotNull(schema);
		assertNotNull(restSchema);
//		assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
//		assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
//		assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());
		// TODO verify other fields
	}

	public void assertSchema(SchemaCreateRequest request, SchemaResponse restSchema) {
		assertNotNull(request);
		assertNotNull(restSchema);
//		assertEquals("The name of the request schema and the name in the returned json do not match.", request.getName(), restSchema.getName());
//		assertEquals("The description of the request and the returned json do not match.", request.getDescription(), restSchema.getDescription());
//		assertEquals("The display name of the request and the returned json do not match.", request.getDisplayName(), restSchema.getDisplayName());
		// TODO assert for schema properties
	}

	public void assertUser(UserCreateRequest request, UserResponse restUser) {
		assertNotNull(request);
		assertNotNull(restUser);

		assertEquals(request.getUsername(), restUser.getUsername());
		assertEquals(request.getEmailAddress(), restUser.getEmailAddress());
		assertEquals(request.getLastname(), restUser.getLastname());
		assertEquals(request.getFirstname(), restUser.getFirstname());

		// TODO check groupuuid vs groups loaded user

	}

	public void assertUser(UserUpdateRequest request, UserResponse restUser) {
		assertNotNull(request);
		assertNotNull(restUser);

		if (request.getUsername() != null) {
			assertEquals(request.getUsername(), restUser.getUsername());
		}

		if (request.getEmailAddress() != null) {
			assertEquals(request.getEmailAddress(), restUser.getEmailAddress());
		}

		if (request.getLastname() != null) {
			assertEquals(request.getLastname(), restUser.getLastname());
		}

		if (request.getFirstname() != null) {
			assertEquals(request.getFirstname(), restUser.getFirstname());
		}
	}

	public void assertGroup(GroupUpdateRequest request, GroupResponse restGroup) {
		assertNotNull(request);
		assertNotNull(restGroup);

		if (request.getName() != null) {
			assertEquals(request.getName(), restGroup.getName());
		}

	}

	/**
	 * Checks whether the given tag is listed within the node rest response.
	 * 
	 * @param restNode
	 * @param tag
	 * @return
	 */
	public boolean containsTag(NodeResponse restNode, Tag tag) {
		assertNotNull(tag);
		assertNotNull(tag.getUuid());
		assertNotNull(restNode);
		if (restNode.getTags() == null) {
			return false;
		}

		for (TagReference restTag : restNode.getTags()) {
			if (tag.getUuid().equals(restTag.getUuid())) {
				return true;
			}
		}
		return false;
	}
}

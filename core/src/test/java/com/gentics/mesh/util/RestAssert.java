package com.gentics.mesh.util;

import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.rest.group.request.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.request.GroupUpdateRequest;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.node.request.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.rest.project.request.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.role.request.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.core.rest.user.request.UserCreateRequest;
import com.gentics.mesh.core.rest.user.request.UserUpdateRequest;
import com.gentics.mesh.core.rest.user.response.UserResponse;

@Component
public class RestAssert {

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private MeshNodeService nodeService;

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
		assertEquals(count(user.getGroups()), restUser.getGroups().size());
		// TODO groups
	}

	public void assertTag(Tag tag, TagResponse restTag) {
		//		tag.setSchema(neo4jTemplate.fetch(tag.getSchema()));
		assertEquals(tag.getUuid(), restTag.getUuid());
		assertEquals(tag.getSchema().getUuid(), restTag.getSchema().getSchemaUuid());
		assertEquals(tag.getSchema().getName(), restTag.getSchema().getSchemaName());
	}

	/**
	 * Compare the create request with a content response.
	 * 
	 * @param request
	 * @param restNode
	 */
	public void assertMeshNode(NodeCreateRequest request, NodeResponse restNode) {

		for (String languageTag : request.getProperties().keySet()) {
			for (Entry<String, String> entry : request.getProperties(languageTag).entrySet()) {
				assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(),
						restNode.getProperty(languageTag, entry.getKey()));
			}
		}

		String schemaName = request.getSchema().getSchemaName();
		assertEquals("The schemaname of the request does not match the response schema name", schemaName, restNode.getSchema().getSchemaName());
		assertEquals(request.getOrder(), restNode.getOrder());
		String tagUuid = request.getParentNodeUuid();
		// TODO how to match the parent tag?

		assertNotNull(restNode.getUuid());
		assertNotNull(restNode.getCreator());
		assertNotNull(restNode.getPerms());

	}

	public void assertMeshNode(NodeCreateRequest request, MeshNode node) {
		assertNotNull(request);
		assertNotNull(node);

		for (String languageTag : request.getProperties().keySet()) {
			for (Entry<String, String> entry : request.getProperties(languageTag).entrySet()) {
				Language language = languageService.findByLanguageTag(languageTag);
				String propValue = nodeService.getProperty(node, language, entry.getKey());
				assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(), propValue);
			}
		}

		assertNotNull(node.getUuid());
		assertNotNull(node.getCreator());
	}

	public void assertMeshNode(MeshNode node, NodeResponse readValue) {
		assertNotNull(node);
		assertNotNull(readValue);
		assertEquals(node.getUuid(), readValue.getUuid());

		//		assertEquals(node.getOrder(), readValue.getOrder());
		assertNotNull(readValue.getPerms());

		Schema schema = node.getSchema();
		//		schema = neo4jTemplate.fetch(schema);
		assertNotNull("The schema of the test object should not be null. No further assertion can be verified.", schema);
		assertEquals(schema.getName(), readValue.getSchema().getSchemaName());
		assertEquals(schema.getUuid(), readValue.getSchema().getSchemaUuid());

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
		assertNotNull(restProject.getPerms());
	}

	public void assertProject(Project project, ProjectResponse restProject) {
		assertNotNull(project);
		assertNotNull(restProject);
		assertNotNull(restProject.getUuid());
		assertNotNull(restProject.getPerms());
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

	public void assertSchema(Schema schema, SchemaResponse restSchema) {
		assertNotNull(schema);
		assertNotNull(restSchema);
		assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
		assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
		assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());
		// TODO verify other fields
	}

	public void assertSchema(ObjectSchemaCreateRequest request, SchemaResponse restSchema) {
		assertNotNull(request);
		assertNotNull(restSchema);
		assertEquals("The name of the request schema and the name in the returned json do not match.", request.getName(), restSchema.getName());
		assertEquals("The description of the request and the returned json do not match.", request.getDescription(), restSchema.getDescription());
		assertEquals("The display name of the request and the returned json do not match.", request.getDisplayName(), restSchema.getDisplayName());
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

		for (TagResponse restTag : restNode.getTags()) {
			if (tag.getUuid().equals(restTag.getUuid())) {
				return true;
			}
		}
		return false;
	}
}

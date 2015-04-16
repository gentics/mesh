package com.gentics.cailun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.core.rest.role.request.RoleCreateRequest;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserResponse;

@Component
public class RestAssert {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private GraphDatabaseService graphDb;

	public void assertGroup(Group group, GroupResponse restGroup) {
		// String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"],\"perms\":[]}";
		// String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"],\"perms\":[]}";
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
		assertNotNull(restGroup.getUsers());
		assertNotNull(restGroup.getUuid());
	}

	public void assertUser(User user, UserResponse restUser) {
		assertEquals(user.getUsername(), restUser.getUsername());
		assertEquals(user.getEmailAddress(), restUser.getEmailAddress());
		assertEquals(user.getFirstname(), restUser.getFirstname());
		assertEquals(user.getLastname(), restUser.getLastname());
		assertEquals(user.getUuid(), restUser.getUuid());
		assertEquals(user.getGroups().size(), restUser.getGroups().size());
		// TODO groups
	}

	public void assertTag(Tag tag, TagResponse restTag) {
		// String json =
		// "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"en\":{\"name\":\"new Name\"}},\"childTags\":[],\"perms\":[\"read\",\"create\",\"update\",\"delete\"]}";

		try (Transaction tx = graphDb.beginTx()) {
			tag.setSchema(neo4jTemplate.fetch(tag.getSchema()));
			tx.success();
		}

		// String json =
		// "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"en\":{\"name\":\"News\"}},\"childTags\":[],\"perms\":[\"read\",\"create\",\"update\",\"delete\"]}";
		assertEquals(tag.getUuid(), restTag.getUuid());
		assertEquals(tag.getSchema().getUuid(), restTag.getSchema().getSchemaUuid());
		assertEquals(tag.getSchema().getName(), restTag.getSchema().getSchemaName());
	}

	/**
	 * Compare the create request with a content response.
	 * 
	 * @param request
	 * @param readValue
	 */
	public void assertContent(ContentCreateRequest request, ContentResponse readValue) {

		for (String languageTag : request.getProperties().keySet()) {
			for (Entry<String, String> entry : request.getProperties(languageTag).entrySet()) {
				assertEquals("The property {" + entry.getKey() + "} did not match with the response object property", entry.getValue(),
						readValue.getProperty(languageTag, entry.getKey()));
			}
		}

		String schemaName = request.getSchemaName();
		assertEquals("The schemaname of the request does not match the response schema name", schemaName, readValue.getSchema().getSchemaName());
		assertEquals(request.getOrder(), readValue.getOrder());
		String tagUuid = request.getTagUuid();
		// TODO how to match the parent tag?

		assertNotNull(readValue.getUuid());
		assertNotNull(readValue.getCreator());
		assertNotNull(readValue.getPerms());
		// String responseJson =
		// "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"en\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"Blessed mealtime again!\"}},\"schemaName\":\"content\",\"perms\":[],\"tags\":[],\"order\":0}";
		// assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);

	}

	@Transactional
	public void assertContent(Content content, ContentResponse readValue) {
		assertNotNull(content);
		assertNotNull(readValue);
		// String json =
		// "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"de\":{\"name\":\"Special News_2014 german\",\"content\":\"Neuigkeiten!\"},\"en\":{\"name\":\"Special News_2014 english\",\"content\":\"News!\"}},\"schemaName\":\"content\",\"perms\":[\"read\",\"create\",\"update\",\"delete\"],\"tags\":[],\"order\":0}";
		// assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
		// String json =
		// "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"de\":{\"name\":\"Special News_2014 german\",\"content\":\"Neuigkeiten!\"}},\"schemaName\":\"content\",\"perms\":[\"read\",\"create\",\"update\",\"delete\"],\"tags\":[],\"order\":0}";
		// assertEqualsSanitizedJson("The response json did not match the expected one", json, response);

		assertEquals(content.getOrder(), readValue.getOrder());
		assertNotNull(readValue.getPerms());

		ObjectSchema schema = content.getSchema();
		schema = neo4jTemplate.fetch(schema);
		assertNotNull("The schema of the test object should not be null. No further assertion can be verified.", schema);
		assertEquals(schema.getName(), readValue.getSchema().getSchemaName());
		assertEquals(schema.getUuid(), readValue.getSchema().getSchemaUuid());

		assertEquals(content.getUuid(), readValue.getUuid());

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
		assertEquals(project.getName(), restProject.getName());
		assertEquals(project.getUuid(), restProject.getUuid());
	}

	public void assertProject(ProjectUpdateRequest request, ProjectResponse restProject) {
		assertNotNull(request);
		assertNotNull(restProject);
		assertNotNull(restProject.getUuid());
		assertEquals(request.getName(), restProject.getName());

	}

	public void assertSchema(ObjectSchema schema, ObjectSchemaResponse restSchema) {

		assertEquals("Name does not match with the requested name.", schema.getName(), restSchema.getName());
		assertEquals("Description does not match with the requested description.", schema.getDescription(), restSchema.getDescription());
		assertEquals("Display names do not match.", schema.getDisplayName(), restSchema.getDisplayName());

		// TODO verify other fields
		// String json =
		// "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"},{\"uuid\":\"uuid-value\",\"name\":\"extraProject\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";
		// String json =
		// "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"Default schema for contents\",\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"content\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"order\":0},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"order\":0}]}";

	}

	public void assertSchema(ObjectSchemaCreateRequest request, ObjectSchemaResponse restSchema) {
		// String json =
		// "{\"uuid\":\"uuid-value\",\"type\":\"object\",\"description\":\"new description\",\"projects\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}],\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"title\":\"new schema name\",\"properties\":[{\"uuid\":\"uuid-value\",\"type\":\"html\",\"key\":\"extra-content\",\"desciption\":\"Some extra content\",\"order\":0}]}";

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

		// String json =
		// "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"new_user\",\"emailAddress\":\"n.user@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]}";
		// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		// assertEquals(newUser.getUsername(), user.getUsername());

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

		// String json = "{\"uuid\":\"uuid-value\",\"name\":\"New Name\",\"roles\":[\"joe1_role\"],\"users\":[\"joe1\"],\"perms\":[]}";
		// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

}

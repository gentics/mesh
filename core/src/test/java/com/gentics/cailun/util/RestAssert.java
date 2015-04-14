package com.gentics.cailun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;

@Component
public class RestAssert {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private GraphDatabaseService graphDb;

	public void assertGroup(Group group, GroupResponse restGroup) {
		// String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"],\"perms\":[]}";
		assertEquals(group.getUuid(), restGroup.getUuid());
		assertEquals(group.getName(), restGroup.getName());
		for (User user : group.getUsers()) {
			assertTrue(restGroup.getUsers().contains(user.getUsername()));
		}
		// TODO roles
		// group.getRoles()
		// TODO perms
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
}

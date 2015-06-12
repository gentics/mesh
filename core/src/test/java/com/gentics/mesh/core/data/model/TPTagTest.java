package com.gentics.mesh.core.data.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.test.AbstractDBTest;

public class TPTagTest extends AbstractDBTest {

	@Autowired
	private TagService tagService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testSimpleTag() {

		Tag tag = tagService.create();

		tag.setName(data().getGerman(), "blub");
		tagService.findByName("dummy", "blub");

		List<String> names = new ArrayList<>();
		names.add("Johannes");
		names.add("Barbara");
		names.add("David");
		names.add("Anna");
		names.add("Clemens");

		Group group = framedGraph.addFramedVertex(Group.class);
		group.setName("testgroup");

		System.out.println(group.getUuid());

		//		for (String name : names) {
		//			User user = framedGraph.addVertex(User.class);
		//			user.setFirstname("firstname value");
		//			user.setLastname("lastname value");
		//			user.setUsername(name);
		//			//			System.out.println("UUID: " + user.getUuid());
		//			//			System.out.println("Firstname: " + user.getFirstname());
		//			//			System.out.println("Lastname: " + user.getLastname());
		//			group.addUser(user);
		//		}
		//
		//		// --- test ---
		//
		////		Iterable<User> users = group.getUsersInOrder("username", 1, 2);
		////		for (User currentUser : users) {
		////			System.out.println(currentUser.getUsername());
		////		}
		//
		//		//		for (TPGroup currentGroup : user.getGroups()) {
		//		//			System.out.println("Found group: " + currentGroup.getName());
		//		//		}
		//		//
		//		//		System.out.println("Group Count:  " + user.getGroupCount());
		//		//
		//		//		Iterable<TPGroup> groupsWithUuid = framedGraph.getVertices("uuid", group.getUuid(), TPGroup.class);
		//		//
		//		//		for (TPGroup currentGroup : groupsWithUuid) {
		//		//			System.out.println("Found By uuuid: " + currentGroup.getUuid() + " " + currentGroup.getId());
		//		//		}
		//		//
		//		//		Neo4j2Vertex node = (Neo4j2Vertex) user.asVertex();
		//		//
		//		//		Node node2 = graphDb.getNodeById((Long) node.getId());
		//		//		assertNotNull(node2);
		//		//		System.out.println(node2.getProperty("firstname"));
		//		//		System.out.println(node2.getId());
		//		//		System.out.println(node2.getProperty("uuid"));
		//		//
		//		//		for (Relationship rel : node2.getRelationships()) {
		//		//			System.out.println("Found rel: " + rel.getType());
		//		//			System.out.println("From: " + rel.getStartNode().getId() + " to " + rel.getEndNode().getId());
		//		//		}
		//		//		// Tag tag = new Tag();
		//		//		// tagService.setProperty(tag, data().getEnglish(), "name", "test");
		//		//		// tagService.save(tag);
	}

}

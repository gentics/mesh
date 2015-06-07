package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertNotNull;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.tinkerpop.TPGroup;
import com.gentics.mesh.core.data.model.tinkerpop.TPUser;
import com.gentics.mesh.test.AbstractDBTest;
import com.google.common.collect.Iterators;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphConfiguration;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.AbstractModule;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;

public class TPTagTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testSimpleTag() {

		Neo4j2Graph graph = new Neo4j2Graph(springConfig.getGraphDatabaseService());
		// FramedGraphFactory factory = new FramedGraphFactory(new GremlinGroovyModule());

		GremlinGroovyModule groovyModule = new GremlinGroovyModule();
		AbstractModule customModule = new AbstractModule() {
			public void doConfigure(FramedGraphConfiguration config) {
				config.addFrameInitializer(new UUIDFrameInitializer());
			}
		};

		FramedGraph<Neo4j2Graph> framedGraph = new FramedGraphFactory(groovyModule, customModule).create(graph);

		// TPTag tag = framedGraph.addVertex(UUIDUtil.randomUUID(), TPTag.class);
		// System.out.println(tag.getUuid());

		TPUser user = framedGraph.addVertex(null, TPUser.class);
		user.setFirstname("firstname value");
		user.setLastname("lastname value");
		System.out.println("UUID: " + user.getUuid());
		System.out.println("Firstname: " + user.getFirstname());
		System.out.println("Lastname: " + user.getLastname());

		TPGroup group = framedGraph.addVertex(null, TPGroup.class);
		group.setName("testgroup");
		group.addUser(user);

		for (TPGroup currentGroup : user.getGroups()) {
			System.out.println("Found group: " + currentGroup.getName());
		}

		System.out.println("Group Count:  " + user.getGroupCount());

		Iterable<TPGroup> groupsWithUuid = framedGraph.getVertices("uuid", group.getUuid(), TPGroup.class);

		for (TPGroup currentGroup : groupsWithUuid) {
			System.out.println("Found By uuuid: " + currentGroup.getUuid() + " " + currentGroup.getId());
		}

		Neo4j2Vertex node = (Neo4j2Vertex) user.asVertex();

		Node node2 = graphDb.getNodeById((Long) node.getId());
		assertNotNull(node2);
		System.out.println(node2.getProperty("firstname"));
		System.out.println(node2.getId());
		System.out.println(node2.getProperty("uuid"));

		for (Relationship rel : node2.getRelationships()) {
			System.out.println("Found rel: " + rel.getType());
			System.out.println("From: " + rel.getStartNode().getId() + " to " + rel.getEndNode().getId());
		}
		// Tag tag = new Tag();
		// tagService.setProperty(tag, data().getEnglish(), "name", "test");
		// tagService.save(tag);
	}

}

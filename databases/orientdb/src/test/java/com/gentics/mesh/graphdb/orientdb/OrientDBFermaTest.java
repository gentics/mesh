
package com.gentics.mesh.graphdb.orientdb;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.ferma.DelegatingFramedOrientGraph;
import com.gentics.mesh.graphdb.ferma.DelegatingFramedTransactionalOrientGraph;
import com.gentics.mesh.graphdb.orientdb.graph.Group;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import io.vertx.core.Vertx;

public class OrientDBFermaTest extends AbstractOrientDBTest {

	private final static int nMembers = 2000;
	private final static int nChecks = 4000;

	private Database db = new OrientDBDatabase();

	@Before
	public void setup() throws Exception {
		db.init(null, Vertx.vertx(), "com.gentics.mesh.graphdb.orientdb.graph");
	}

	@Test
	public void testOrientVerticleClass() {
		try (Tx tx = db.tx()) {
			Person p = tx.getGraph().addFramedVertex(Person.class);
			p.setName("personName");
			System.out.println(p.getId());
			System.out.println(p.getElement().getId());
			System.out.println(((OrientVertex) p.getElement()).getBaseClassName());
			System.out.println(((OrientVertex) p.getElement()).getElementType());
			System.out.println(((OrientVertex) p.getElement()).getType());
			System.out.println(((OrientVertex) p.getElement()).getLabel());
			System.out.println(p.getElement().getClass().getName());
			tx.success();
		}
	}

	private void setupTypesAndIndices() {
		try (NoTx tx = db.noTx()) {
			OrientGraphNoTx g = ((OrientGraphNoTx) ((DelegatingFramedOrientGraph<?>) tx.getGraph()).getBaseGraph());
			// g.setUseClassForEdgeLabel(true);
			g.setUseLightweightEdges(false);
			g.setUseVertexFieldsForEdgeLabels(false);
		}

		try (NoTx tx = db.noTx()) {
			OrientGraphNoTx g = ((OrientGraphNoTx) ((DelegatingFramedOrientGraph<?>) tx.getGraph()).getBaseGraph());
			System.out.println(g.getClass().getName());

			OrientEdgeType e = g.createEdgeType("HAS_MEMBER");
			e.createProperty("in", OType.LINK);
			e.createProperty("out", OType.LINK);
			e.createIndex("e.has_member", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "out", "in");

			OrientVertexType v = g.createVertexType(Group.class.getSimpleName(), "V");
			v.createProperty("name", OType.STRING);

			v = g.createVertexType(Person.class.getSimpleName(), "V");
			v.createProperty("name", OType.STRING);
			v.createIndex(Person.class.getSimpleName() + ".name", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "name");
		}

	}

	@Test
	@Ignore
	public void testFermaIndexUsage() {

		setupTypesAndIndices();

		List<Person> persons = new ArrayList<>();
		Group g;
		try (Tx tx = db.tx()) {
			g = tx.getGraph().addFramedVertex(Group.class);
			g.setName("groupName");
			for (int i = 0; i < nMembers; i++) {
				Person p = tx.getGraph().addFramedVertex(Person.class);
				p.setName("personName_" + i);
				g.addMember(p);
				persons.add(p);
			}
			tx.success();
		}

		try (Tx tx = db.tx()) {
			long start = System.currentTimeMillis();
			OrientGraph graph = ((OrientGraph) ((DelegatingFramedTransactionalOrientGraph<?>) tx.getGraph()).getBaseGraph());
			// assertEquals(nMembers, g.getMembers().size());
			for (int i = 0; i < nChecks; i++) {
				int nPerson = (int) (Math.random() * persons.size());
				String name = "personName_" + nPerson;
				//				assertEquals(name, tx.getGraph().getFramedVerticesExplicit("Person.name", name, Person.class).iterator().next().getName());
				//				assertTrue(tx.getGraph().getFramedVerticesExplicit("Person.name", name, Person.class).iterator().hasNext());

				//				Iterable<Vertex> vertices = graph.getVertices(Person.class.getSimpleName(), new String[] { "name" },new Object[] {name});
				//				 assertTrue(vertices.iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			double perCheck = ((double) dur / (double) nChecks);
			System.out.println("[graph.getVertices] Duration per lookup: " + perCheck);
			System.out.println("[graph.getVertices] Duration: " + dur);

		}
	}

}

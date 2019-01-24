/**
 * Copyright 2004 - 2017 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * This product currently only contains code developed by authors
 * of specific components, as identified by the source code files.
 *
 * Since product implements StAX API, it has dependencies to StAX API
 * classes.
 *
 * For additional credits (generally to people who reported problems)
 * see CREDITS file.
 */

package com.syncleus.ferma.ext.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.syncleus.ferma.ext.orientdb.model.Group;
import com.syncleus.ferma.ext.orientdb.model.Person;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class IndexTest extends AbstractOrientDBTest {

	private final static int nMembers = 2000;
	private final static int nChecks = 4000;

	@Test
	public void testOrientVerticleClass() {
		try (Tx tx = graph.tx()) {
			Person p = tx.getGraph().addFramedVertex(Person.class);
			p.setName("personName");
			assertEquals(Person.class.getSimpleName(), ((OrientVertex) p.getElement()).getLabel());
			tx.success();
		}
	}

	/**
	 * Setup some indices. This is highly orientdb specific and may not be easy so setup using blueprint API.
	 */
	private void setupTypesAndIndices() {
		try (Tx tx = graph.tx()) {
			OrientGraph g = ((OrientGraph) ((DelegatingFramedOrientGraph) tx.getGraph()).getBaseGraph());

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
	public void testFermaIndexUsage() {

		setupTypesAndIndices();

		// Create a group with x persons assigned to it.
		List<Person> persons = new ArrayList<>();
		Group g;
		try (Tx tx = graph.tx()) {
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

		try (Tx tx = graph.tx()) {
			assertEquals(nMembers, g.getMembers().size());
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				int nPerson = (int) (Math.random() * persons.size());
				String name = "personName_" + nPerson;
				assertTrue(tx.getGraph().getFramedVerticesExplicit("Person.name", name, Person.class).iterator().hasNext());
			}
			long dur = System.currentTimeMillis() - start;
			double perCheck = ((double) dur / (double) nChecks);
			System.out.println("[graph.getVertices] Duration per lookup: " + perCheck);
			System.out.println("[graph.getVertices] Duration: " + dur);

		}
	}

}

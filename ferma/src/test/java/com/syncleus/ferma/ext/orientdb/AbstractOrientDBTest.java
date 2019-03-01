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

import org.junit.Before;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.ext.orientdb.impl.OrientTransactionFactoryImpl;
import com.syncleus.ferma.ext.orientdb.model.Person;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class AbstractOrientDBTest {

	protected OrientGraphFactory graphFactory;
	protected OrientTransactionFactoryImpl graph;

	@Before
	public void setupDB() {
		graphFactory = new OrientGraphFactory("memory:tinkerpop" + System.currentTimeMillis()).setupPool(4, 10);
		graph = new OrientTransactionFactoryImpl(graphFactory, "com.syncleus.ferma.ext.orientdb.model");
	}

	/**
	 * Update the person name and add 10 more friends. Update the names of all friends.
	 * 
	 * @param graph
	 * @param person
	 */
	public void manipulatePerson(FramedGraph graph, Person person) {
		person.setName("Changed " + System.currentTimeMillis());
		for (int i = 0; i < 10; i++) {
			Person friend = graph.addFramedVertex(Person.class);
			friend.setName("Friend " + i);
			person.addFriend(friend);
		}
		for (Person friend : person.getFriends()) {
			friend.setName("Changed Name " + System.currentTimeMillis());
		}
	}

	/**
	 * Add a friend to the provided person
	 * 
	 * @param graph
	 * @param person
	 */
	public void addFriend(FramedGraph graph, Person person) {
		Person friend = graph.addFramedVertex(Person.class);
		friend.setName("NewFriend");
		person.addFriend(friend);
	}

	/**
	 * Create a single person which has 10 friends (11 Vertices will be created)
	 * 
	 * @param graph
	 * @param name
	 * @return Created person
	 */
	public Person addPersonWithFriends(FramedGraph graph, String name) {
		Person p = graph.addFramedVertex(Person.class);
		p.setName(name);

		for (int i = 0; i < 10; i++) {
			Person friend = graph.addFramedVertex(Person.class);
			friend.setName("Friend " + i);
			p.addFriend(friend);
		}
		return p;
	}

}

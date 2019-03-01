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

import static com.syncleus.ferma.ext.orientdb.util.TestUtils.runAndWait;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.syncleus.ferma.ext.orientdb.model.Person;
import com.syncleus.ferma.tx.Tx;

public class MultithreadingReducedTest extends AbstractOrientDBTest {

	private Person p;

	@Before
	public void setup() {
		setupData();
	}

	private void setupData() {
		try (Tx tx = graph.tx()) {
			String name = "SomeName";
			p = addPersonWithFriends(tx.getGraph(), name);
			tx.success();
			runAndWait(() -> {
				try (Tx tx2 = graph.tx()) {
					readPerson(p);
					manipulatePerson(tx2.getGraph(), p);
				}
			});
		}

		runAndWait(() -> {
			try (Tx tx2 = graph.tx()) {
				readPerson(p);
				manipulatePerson(tx2.getGraph(), p);
			}
		});

	}

	@Test
	public void testMultithreading() {

		runAndWait(() -> {
			Person reloaded;
			try (Tx tx = graph.tx()) {
				manipulatePerson(tx.getGraph(), p);
				String name = "newName";
				p.setName(name);
				reloaded = tx.getGraph().v().has(Person.class).has("name", name).nextOrDefaultExplicit(Person.class, null);
				System.out.println(reloaded.getName());
				assertNotNull(reloaded);
				manipulatePerson(tx.getGraph(), reloaded);
				tx.success();
			}
			runAndWait(() -> {
				try (Tx tx2 = graph.tx()) {
					readPerson(reloaded);
				}
			});
		});
	}

	private void readPerson(Person person) {
		person.getName();
		for (Person p : person.getFriends()) {
			p.getName();
			for (Person p2 : person.getFriends()) {
				p2.getName();
				for (Person p3 : p2.getFriends()) {
					p3.getName();
				}
			}
		}
	}

}

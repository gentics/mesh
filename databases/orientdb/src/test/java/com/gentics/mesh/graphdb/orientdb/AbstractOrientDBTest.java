package com.gentics.mesh.graphdb.orientdb;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;

public class AbstractOrientDBTest {

	protected void manipulatePerson(Tx tx, Person p) {
		p.setName("Changed " + System.currentTimeMillis());
		for (int i = 0; i < 10; i++) {
			Person friend = tx.createVertex(Person.class);
			friend.setName("Friend " + i);
			p.addFriend(friend);
		}
		for (Person friend : p.getFriends()) {
			friend.setName("Changed Name " + System.currentTimeMillis());
		}
	}

	protected void addFriend(Tx tx, Person p) {
		Person friend = tx.createVertex(Person.class);
		friend.setName("NewFriend");
		p.addFriend(friend);
	}

	protected Person addPersonWithFriends(Tx tx, String name) {
		Person p = tx.createVertex(Person.class);
		p.setName(name);

		for (int i = 0; i < 10; i++) {
			Person friend = tx.createVertex(Person.class);
			friend.setName("Friend " + i);
			p.addFriend(friend);
		}
		return p;
	}
}

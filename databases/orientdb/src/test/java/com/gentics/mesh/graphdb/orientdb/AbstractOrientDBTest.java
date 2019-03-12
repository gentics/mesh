package com.gentics.mesh.graphdb.orientdb;

import org.mockito.Mockito;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.ResettableCounter;
import com.syncleus.ferma.FramedGraph;

public class AbstractOrientDBTest {

	protected Database mockDatabase() {
		MetricsService metrics = Mockito.mock(MetricsService.class);
		Mockito.when(metrics.timer(Mockito.any())).thenReturn(Mockito.mock(Timer.class));
		Mockito.when(metrics.counter(Mockito.any())).thenReturn(Mockito.mock(Counter.class));
		Mockito.when(metrics.meter(Mockito.any())).thenReturn(Mockito.mock(Meter.class));
		Mockito.when(metrics.resetableCounter(Mockito.any())).thenReturn(Mockito.mock(ResettableCounter.class));
		Database db = new OrientDBDatabase(metrics);
		return db;
	}

	protected void manipulatePerson(FramedGraph graph, Person p) {
		p.setName("Changed " + System.currentTimeMillis());
		for (int i = 0; i < 10; i++) {
			Person friend = graph.addFramedVertex(Person.class);
			friend.setName("Friend " + i);
			p.addFriend(friend);
		}
		for (Person friend : p.getFriends()) {
			friend.setName("Changed Name " + System.currentTimeMillis());
		}
	}

	protected void addFriend(FramedGraph graph, Person p) {
		Person friend = graph.addFramedVertex(Person.class);
		friend.setName("NewFriend");
		p.addFriend(friend);
	}

	protected Person addPersonWithFriends(FramedGraph graph, String name) {
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

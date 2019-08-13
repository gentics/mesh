package com.gentics.mesh.graphdb.orientdb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.ResettableCounter;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;

public class AbstractOrientDBTest {

	protected Database mockDatabase() {
		MetricsService metrics = Mockito.mock(MetricsService.class);
		when(metrics.timer(Mockito.any())).thenReturn(Mockito.mock(Timer.class));
		when(metrics.counter(Mockito.any())).thenReturn(Mockito.mock(Counter.class));
		when(metrics.meter(Mockito.any())).thenReturn(Mockito.mock(Meter.class));
		when(metrics.resetableCounter(Mockito.any())).thenReturn(Mockito.mock(ResettableCounter.class));
		Lazy<BootstrapInitializer> lazyBoot = mock(Lazy.class);
		BootstrapInitializer bootMock = mock(BootstrapInitializer.class);
		when(lazyBoot.get()).thenReturn(bootMock);
		Database db = new OrientDBDatabase(null, lazyBoot, metrics, null, null, new OrientDBClusterManager(null, null, null, null));
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

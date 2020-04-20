package com.gentics.mesh.graphdb.orientdb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

public class AbstractOrientDBTest {

	protected Database mockDatabase() {
		MetricsService metrics = Mockito.mock(MetricsService.class);
		when(metrics.timer(Mockito.any())).thenReturn(Mockito.mock(Timer.class));
		when(metrics.counter(Mockito.any())).thenReturn(Mockito.mock(Counter.class));
		Lazy<BootstrapInitializer> lazyBoot = mock(Lazy.class);
		BootstrapInitializer bootMock = mock(BootstrapInitializer.class);
		when(lazyBoot.get()).thenReturn(bootMock);
		MeshOptions options = new MeshOptions();
		Database db = new OrientDBDatabase(null, lazyBoot, metrics, null, null, new OrientDBClusterManager(null, null, null, options, null), null, null, null);
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

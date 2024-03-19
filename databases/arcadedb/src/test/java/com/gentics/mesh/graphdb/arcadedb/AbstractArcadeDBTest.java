package com.gentics.mesh.graphdb.arcadedb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.graphdb.ArcadeDBDatabase;
import com.gentics.mesh.graphdb.arcadedb.graph.Person;
import com.gentics.mesh.graphdb.ferma.PermissionRootsImpl;
import com.gentics.mesh.metric.MetricsService;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

public class AbstractArcadeDBTest {

	protected Database mockDatabase(GraphDBMeshOptions options) {
		MetricsService metrics = Mockito.mock(MetricsService.class);
		when(metrics.timer(Mockito.any())).thenReturn(Mockito.mock(Timer.class));
		when(metrics.counter(Mockito.any())).thenReturn(Mockito.mock(Counter.class));
		Lazy<DaoCollection> lazyDaos = mock(Lazy.class);

		Lazy<BootstrapInitializer> lazyBoot = mock(Lazy.class);
		BootstrapInitializer bootMock = mock(BootstrapInitializer.class);
		when(lazyBoot.get()).thenReturn(bootMock);

		PermissionRoots permRoots = mock(PermissionRootsImpl.class);
		Lazy<PermissionRoots> lazyPermRoots = mock(Lazy.class);
		when(lazyPermRoots.get()).thenReturn(permRoots);

		Database db = new ArcadeDBDatabase(options, null, lazyBoot, lazyDaos, metrics, null, null,
			null,
			null, lazyPermRoots, null, null, null);
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

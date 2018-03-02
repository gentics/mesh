package com.gentics.mesh.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.gentics.mesh.rest.impl.InternalEndpointRouteImpl;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class EndpointTest {

	@Test
	public void testRAMLPath() {
		Router router = mock(Router.class);
		Route route = mock(Route.class);
		when(router.route()).thenReturn(route);
		InternalEndpointRoute e = new InternalEndpointRouteImpl(router);

		when(route.getPath()).thenReturn("/:bla/:blub/:blar");
		assertEquals("/{bla}/{blub}/{blar}", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla", "blub", "blar");

		when(route.getPath()).thenReturn("/:bla/blub");
		assertEquals("/{bla}/blub", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/:bla/blub/");
		assertEquals("/{bla}/blub/", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/:bla/blub/test");
		assertEquals("/{bla}/blub/test", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/:bla/blub/test/");
		assertEquals("/{bla}/blub/test/", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/:bla/blub/test/:moep");
		assertEquals("/{bla}/blub/test/{moep}", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla", "moep");

		when(route.getPath()).thenReturn("/:bla/:blub/");
		assertEquals("/{bla}/{blub}/", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla", "blub");

		when(route.getPath()).thenReturn("/:bla/:blub");
		assertEquals("/{bla}/{blub}", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla", "blub");

		when(route.getPath()).thenReturn("/:bla/");
		assertEquals("/{bla}/", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/:bla");
		assertEquals("/{bla}", e.getRamlPath());
		assertThat(e.getNamedSegments()).containsExactly("bla");

		when(route.getPath()).thenReturn("/bla");
		assertEquals("/bla", e.getRamlPath());
		assertThat(e.getNamedSegments()).isEmpty();

		when(route.getPath()).thenReturn("/");
		assertEquals("/", e.getRamlPath());
		assertThat(e.getNamedSegments()).isEmpty();
	}
}

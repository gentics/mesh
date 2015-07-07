package com.gentics.mesh.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import java.io.IOException;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.RestAssert;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractDBTest {

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	@Autowired
	protected RestAssert test;

	@Autowired
	private I18NService i18n;

	public void setupData() throws JsonParseException, JsonMappingException, IOException {
		purgeDatabase();
		dataProvider.setup(1);
	}

	public DemoDataProvider data() {
		return dataProvider;
	}

	protected void purgeDatabase() {
		fg.commit();
		for (Edge edge : fg.getEdges()) {
			edge.remove();
		}
		for (Vertex vertex : fg.getVertices()) {
			vertex.remove();
		}
	}

	protected RoutingContext getMockedRoutingContext(String query) {

		User user = data().getUserInfo().getUser();

		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);

		MeshAuthUserImpl requestUser = fg.frameElement(user.getElement(), MeshAuthUserImpl.class);

		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);
		JsonObject principal = new JsonObject();
		principal.put("uuid", user.getUuid());
		when(rc.user()).thenReturn(requestUser);
		//when(vertxUser.principal()).thenReturn(principal);
		// Create login session
		// String loginSessionId = auth.createLoginSession(Long.MAX_VALUE, user);
		// String loginSessionId = null;
		// Session session = mock(Session.class);
		// RoutingContext rc = mock(RoutingContext.class);
		// when(rc.session()).thenReturn(session);
		// when(session.id()).thenReturn(loginSessionId);
		return rc;
	}

}

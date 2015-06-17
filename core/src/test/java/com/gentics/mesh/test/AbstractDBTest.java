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
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.verticle.UserVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.RestAssert;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService languageService;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	protected RoleService roleService;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	protected FramedGraph framedGraph;

	@Autowired
	protected UserVerticle userVerticle;

	@Autowired
	protected UserService userService;

	@Autowired
	protected GroupService groupService;

	@Autowired
	protected MeshNodeService nodeService;

	@Autowired
	protected RestAssert test;

	@Autowired
	private I18NService i18n;

	public void setupData() throws JsonParseException, JsonMappingException, IOException {
		purgeDatabase();
		//		try (Transaction tx = graphDb.beginTx()) {
		dataProvider.setup(1);
		//			tx.success();
		//		}
	}

	public DemoDataProvider data() {
		return dataProvider;
	}

	protected void purgeDatabase() {
		//		try (Transaction tx = graphDb.beginTx()) {
		for (Edge edge : framedGraph.getEdges()) {
			edge.remove();
		}
		for (Vertex vertex : framedGraph.getVertices()) {
			vertex.remove();
		}
		//			tx.success();
		//		}
	}

	protected RoutingContext getMockedRoutingContext(String query) {

		MeshUser user = data().getUserInfo().getUser();

		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);

		io.vertx.ext.auth.User vertxUser = mock(io.vertx.ext.auth.User.class);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);
		JsonObject principal = new JsonObject();
		principal.put("uuid", user.getUuid());
		when(vertxUser.principal()).thenReturn(principal);
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

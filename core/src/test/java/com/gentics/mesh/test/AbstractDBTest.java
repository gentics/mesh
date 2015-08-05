package com.gentics.mesh.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
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
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			purgeDatabase();
			tx.success();
		}
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			dataProvider.setup(1);
			tx.success();
			fg.commit();
		}
	}

	@Deprecated
	public DemoDataProvider data() {
		return dataProvider;
	}

	public SchemaContainer schemaContainer(String key) {
		return data().getSchemaContainer(key);
	}

	public Map<String, ? extends Tag> tags() {
		return data().getTags();
	}

	public Tag tag(String key) {
		return data().getTag(key);
	}

	public TagFamily tagFamily(String key) {
		return data().getTagFamily(key);
	}

	public Project project() {
		return data().getProject();
	}

	public Node content(String key) {
		return data().getContent(key);
	}

	public Node folder(String key) {
		return data().getFolder(key);
	}

	public Map<String, User> users() {
		return data().getUsers();
	}

	public Map<String, Role> roles() {
		return data().getRoles();
	}

	public Map<String, TagFamily> tagFamilies() {
		return data().getTagFamilies();
	}

	public Map<String, Group> groups() {
		return data().getGroups();
	}

	public Language english() {
		return data().getEnglish();
	}

	public Language german() {
		return data().getGerman();
	}

	public User user() {
		return data().getUserInfo().getUser();
	}

	public String password() {
		return data().getUserInfo().getPassword();
	}

	public Group group() {
		return data().getUserInfo().getGroup();
	}

	public Role role() {
		return data().getUserInfo().getRole();
	}

	public MeshRoot meshRoot() {
		return data().getMeshRoot();
	}

	public Node content() {
		return data().getContent("news overview");
	}

	public MeshAuthUser getRequestUser() {
		return data().getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
	}

	public SchemaContainer getSchemaContainer() {
		return data().getSchemaContainer("content");
	}

	protected void purgeDatabase() {
		// fg.commit();
		for (Edge edge : fg.getEdges()) {
			edge.remove();
		}
		for (Vertex vertex : fg.getVertices()) {
			vertex.remove();
		}
	}

	protected String getJson(Node node) throws InterruptedException {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> reference = new AtomicReference<>();
		node.transformToRest(rc, rh -> {
			NodeResponse response = rh.result();
			reference.set(JsonUtil.toJson(response));
			assertNotNull(response);
			latch.countDown();
		});
		latch.await();
		return reference.get();
	}

	protected RoutingContext getMockedRoutingContext(String query) {

		User user = data().getUserInfo().getUser();
		Map<String, Object> map = new HashMap<>();
		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);

		MeshAuthUserImpl requestUser = fg.frameElement(user.getElement(), MeshAuthUserImpl.class);
		when(rc.data()).thenReturn(map);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);
		JsonObject principal = new JsonObject();
		principal.put("uuid", user.getUuid());
		when(rc.user()).thenReturn(requestUser);

		return rc;
	}

}

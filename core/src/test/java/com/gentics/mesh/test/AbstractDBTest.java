package com.gentics.mesh.test;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestAssert;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

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
	protected Database db;

	@Autowired
	protected DatabaseService databaseService;

	@Autowired
	protected RestAssert test;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		JsonUtil.debugMode = true;
	}

	public void setupData() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		dataProvider.setup(1);
		dataProvider.updatePermissions();
	}

	@Deprecated
	public DemoDataProvider data() {
		return dataProvider;
	}

	public SchemaContainer schemaContainer(String key) {
		SchemaContainer container = data().getSchemaContainer(key);
		container.reload();
		return container;

	}

	public Map<String, ? extends Tag> tags() {
		return data().getTags();
	}

	public Tag tag(String key) {
		Tag tag = data().getTag(key);
		tag.reload();
		return tag;
	}

	public TagFamily tagFamily(String key) {
		TagFamily family = data().getTagFamily(key);
		family.reload();
		return family;
	}

	public Project project() {
		Project project = data().getProject();
		project.reload();
		return project;
	}

	public Node content(String key) {
		Node node = data().getContent(key);
		node.reload();
		return node;

	}

	public Node folder(String key) {
		Node node = data().getFolder(key);
		node.reload();
		return node;
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
		User user = data().getUserInfo().getUser();
		user.reload();
		return user;
	}

	public String password() {
		return data().getUserInfo().getPassword();
	}

	public Group group() {
		Group group = data().getUserInfo().getGroup();
		group.reload();
		return group;
	}

	public Role role() {
		Role role = data().getUserInfo().getRole();
		role.reload();
		return role;
	}

	public MeshRoot meshRoot() {
		return data().getMeshRoot();
	}

	public Node content() {
		Node content = data().getContent("news overview");
		content.reload();
		return content;
	}

	public MeshAuthUser getRequestUser() {
		return data().getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
	}

	public SchemaContainer getSchemaContainer() {
		SchemaContainer container = data().getSchemaContainer("content");
		container.reload();
		return container;
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
		failingLatch(latch);
		return reference.get();
	}

	protected RoutingContext getMockedRoutingContext(String query) {
		try (Trx tx = new Trx(db)) {
			User user = data().getUserInfo().getUser();
			Map<String, Object> map = new HashMap<>();
			RoutingContext rc = mock(RoutingContext.class);
			Session session = mock(Session.class);
			HttpServerRequest request = mock(HttpServerRequest.class);
			when(request.query()).thenReturn(query);

			MeshAuthUserImpl requestUser = tx.getGraph().frameElement(user.getElement(), MeshAuthUserImpl.class);
			when(rc.data()).thenReturn(map);
			when(rc.request()).thenReturn(request);
			when(rc.session()).thenReturn(session);
			JsonObject principal = new JsonObject();
			principal.put("uuid", user.getUuid());
			when(rc.user()).thenReturn(requestUser);
			return rc;
		}
	}

}

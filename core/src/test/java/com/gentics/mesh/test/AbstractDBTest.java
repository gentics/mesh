package com.gentics.mesh.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.RestAssert;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public abstract class AbstractDBTest {

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	private TestDataProvider dataProvider;

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

	protected void resetDatabase() {
		BootstrapInitializer.clearReferences();
		Database db = databaseService.getDatabase();
		db.clear();
		DatabaseHelper helper = new DatabaseHelper(db);
		helper.init();
		// databaseService.getDatabase().reset();

	}

	public void setupData() throws Exception {
		dataProvider.setup();
	}

	public MicroschemaContainer microschemaContainer(String key) {
		MicroschemaContainer container = dataProvider.getMicroschemaContainers().get(key);
		container.reload();
		return container;
	}

	public SchemaContainer schemaContainer(String key) {
		SchemaContainer container = dataProvider.getSchemaContainer(key);
		container.reload();
		return container;
	}

	public UserInfo getUserInfo() {
		return dataProvider.getUserInfo();
	}

	public Map<String, ? extends Tag> tags() {
		return dataProvider.getTags();
	}

	public Tag tag(String key) {
		Tag tag = dataProvider.getTag(key);
		tag.reload();
		return tag;
	}

	public TagFamily tagFamily(String key) {
		TagFamily family = dataProvider.getTagFamily(key);
		family.reload();
		return family;
	}

	public Project project() {
		Project project = dataProvider.getProject();
		project.reload();
		return project;
	}

	public Node content(String key) {
		Node node = dataProvider.getContent(key);
		node.reload();
		return node;
	}

	public Node folder(String key) {
		Node node = dataProvider.getFolder(key);
		node.reload();
		return node;
	}

	public Map<String, User> users() {
		return dataProvider.getUsers();
	}

	public Map<String, Role> roles() {
		return dataProvider.getRoles();
	}

	public Map<String, TagFamily> tagFamilies() {
		return dataProvider.getTagFamilies();
	}

	public Map<String, Group> groups() {
		return dataProvider.getGroups();
	}

	public Map<String, SchemaContainer> schemaContainers() {
		return dataProvider.getSchemaContainers();
	}

	public Map<String, MicroschemaContainer> microschemaContainers() {
		return dataProvider.getMicroschemaContainers();
	}

	public int getNodeCount() {
		return dataProvider.getNodeCount();
	}

	public Language english() {
		Language language = dataProvider.getEnglish();
		language.reload();
		return language;
	}

	public Language german() {
		Language language = dataProvider.getGerman();
		language.reload();
		return language;
	}

	public User user() {
		User user = dataProvider.getUserInfo().getUser();
		user.reload();
		return user;
	}

	public String password() {
		return dataProvider.getUserInfo().getPassword();
	}

	public Group group() {
		Group group = dataProvider.getUserInfo().getGroup();
		group.reload();
		return group;
	}

	public Role role() {
		Role role = dataProvider.getUserInfo().getRole();
		role.reload();
		return role;
	}

	public MeshRoot meshRoot() {
		return dataProvider.getMeshRoot();
	}

	public SearchQueueBatch createBatch() {
		return meshRoot().getSearchQueue().createBatch(UUIDUtil.randomUUID());
	}

	public Node content() {
		Node content = dataProvider.getContent("news overview");
		content.reload();
		return content;
	}

	public MeshAuthUser getRequestUser() {
		return dataProvider.getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
	}

	public SchemaContainer getSchemaContainer() {
		SchemaContainer container = dataProvider.getSchemaContainer("content");
		container.reload();
		return container;
	}

	protected String getJson(Node node) throws Exception {
		RoutingContext rc = getMockedRoutingContext("lang=en&version=draft");
		InternalActionContext ac = InternalActionContext.create(rc);
		return JsonUtil.toJson(node.transformToRest(ac, 0).toBlocking().single());
	}

	protected InternalActionContext getMockedVoidInternalActionContext(String query) {
		InternalActionContext ac = InternalActionContext.create(getMockedRoutingContext(query, true));
		return ac;
	}

	protected InternalActionContext getMockedInternalActionContext(String query) {
		InternalActionContext ac = InternalActionContext.create(getMockedRoutingContext(query, false));
		return ac;
	}

	protected RoutingContext getMockedRoutingContext(String query) {
		return getMockedRoutingContext(query, false);
	}

	protected RoutingContext getMockedRoutingContext(String query, boolean noInternalMap) {
		User user = dataProvider.getUserInfo().getUser();
		Map<String, Object> map = new HashMap<>();
		if (noInternalMap) {
			map = null;
		}
		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);
		Map<String, String> paramMap = HttpQueryUtils.splitQuery(query);
		paramMap.entrySet().stream().forEach(entry -> when(request.getParam(entry.getKey())).thenReturn(entry.getValue()));
		MeshAuthUserImpl requestUser = Database.getThreadLocalGraph().frameElement(user.getElement(), MeshAuthUserImpl.class);
		when(rc.data()).thenReturn(map);
		MultiMap headerMap = mock(MultiMap.class);
		when(headerMap.get("Accept-Language")).thenReturn("en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);
		JsonObject principal = new JsonObject();
		principal.put("uuid", user.getUuid());
		when(rc.user()).thenReturn(requestUser);
		when(rc.get(RouterStorage.PROJECT_CONTEXT_KEY)).thenReturn(project().getName());
		return rc;

	}

}

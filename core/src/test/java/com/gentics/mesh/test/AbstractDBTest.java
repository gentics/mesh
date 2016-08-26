package com.gentics.mesh.test;

import java.util.Map;

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
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.dagger.TestMeshComponent;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.test.dagger.MeshTestModule;
import com.gentics.mesh.util.RestAssert;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractDBTest {

	protected BootstrapInitializer boot;

	private TestDataProvider dataProvider;

	protected Database db;

	protected RestAssert test = new RestAssert();

	protected TestMeshComponent meshDagger;

	protected RouterStorage routerStorage;
	
	protected ServerSchemaStorage schemaStorage;
	
	protected DummySearchProvider searchProvider;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	public void initDagger() throws Exception {
		MeshTestModule.init();
		meshDagger = MeshCore.createTest();
		dataProvider = meshDagger.testDataProvider();
		routerStorage = meshDagger.routerStorage();
		searchProvider = meshDagger.dummySearchProvider();
		schemaStorage = meshDagger.serverSchemaStorage();
		boot = meshDagger.boot();
		db = meshDagger.database();
	}

	protected void resetDatabase() {
		BootstrapInitializer.clearReferences();
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

	/**
	 * Returns the news overview node which has no tags.
	 * 
	 * @return
	 */
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
		RoutingContext rc = Mocks.getMockedRoutingContext("lang=en&version=draft", user());
		InternalActionContext ac = InternalActionContext.create(rc);
		ac.data().put(RouterStorage.PROJECT_CONTEXT_KEY, TestDataProvider.PROJECT_NAME);
		return JsonUtil.toJson(node.transformToRest(ac, 0).toBlocking().value());
	}

}

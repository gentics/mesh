package com.gentics.mesh.test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.cache.PermissionStore;
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
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Central abstract class for all mesh unit tests which access dagger or the mesh test database graph.
 */
public abstract class AbstractDBTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractDBTest.class);

	public static BootstrapInitializer boot;

	public static TestDataProvider dataProvider;

	public static Database db;

	public static MeshComponent meshDagger;

	public static RouterStorage routerStorage;

	public static ServerSchemaStorage schemaStorage;

	public static SearchProvider searchProvider;

	public static DummySearchProvider dummySearchProvider;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	/**
	 * Initialise mesh only once. The dagger context will only be setup once.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void initMesh() throws Exception {
		init(false);
		initDagger();
	}

	/**
	 * Initialise mesh options.
	 * 
	 * @param enableES
	 * @throws IOException
	 */
	public static void init(boolean enableES) throws IOException {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		String uploads = "target/testuploads_" + UUIDUtil.randomUUID();
		File uploadDir = new File(uploads);
		FileUtils.deleteDirectory(uploadDir);
		uploadDir.mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/tmp_" + UUIDUtil.randomUUID();
		File tmpDir = new File(targetTmpDir);
		FileUtils.deleteDirectory(tmpDir);
		tmpDir.mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/image_cache_" + UUIDUtil.randomUUID();
		File cacheDir = new File(imageCacheDir);
		FileUtils.deleteDirectory(cacheDir);
		cacheDir.mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);

		ElasticSearchOptions searchOptions = new ElasticSearchOptions();
		if (enableES) {
			searchOptions.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
		} else {
			searchOptions.setDirectory(null);
		}
		searchOptions.setHttpEnabled(true);
		options.setSearchOptions(searchOptions);
		Mesh.mesh(options);
	}

	/**
	 * Initialise the mesh dagger context and inject the dependencies within the test.
	 */
	public static void initDagger() {
		log.info("Initializing dagger context");
		meshDagger = MeshInternal.create();
		dataProvider = new TestDataProvider(meshDagger.boot(), meshDagger.database());
		routerStorage = meshDagger.routerStorage();
		if (meshDagger.searchProvider() instanceof DummySearchProvider) {
			dummySearchProvider = meshDagger.dummySearchProvider();
		}
		searchProvider = meshDagger.searchProvider();
		schemaStorage = meshDagger.serverSchemaStorage();
		boot = meshDagger.boot();
		db = meshDagger.database();
		new DatabaseHelper(db).init();
	}

	@After
	public void cleanupFolders() throws IOException {
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getImageOptions().getImageCacheDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory()));
		// if (Mesh.mesh().getOptions().getSearchOptions().getDirectory() != null) {
		// FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getSearchOptions().getDirectory()));
		// }
		PermissionStore.invalidate();
	}

	/**
	 * Clear the test data.
	 */
	protected void resetDatabase() {
		BootstrapInitializerImpl.clearReferences();
		long start = System.currentTimeMillis();
		db.clear();
		long duration = System.currentTimeMillis() - start;
		log.info("Clearing DB took {" + duration + "} ms.");
		if (dummySearchProvider != null) {
			dummySearchProvider.reset();
		}
	}

	/**
	 * Setup the test data.
	 * 
	 * @throws Exception
	 */
	public void setupData() throws Exception {
		db.setMassInsertIntent();
		boot.createSearchIndicesAndMappings();
		dataProvider.setup();
		db.resetIntent();
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
		return MeshInternal.get().searchQueue().createBatch();
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
		return dataProvider.getUserInfo().getUser().reframe(MeshAuthUserImpl.class);
	}

	public SchemaContainer getSchemaContainer() {
		SchemaContainer container = dataProvider.getSchemaContainer("content");
		container.reload();
		return container;
	}

	protected String getJson(Node node) throws Exception {
		RoutingContext rc = Mocks.getMockedRoutingContext("lang=en&version=draft", user());
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		ac.data().put(RouterStorage.PROJECT_CONTEXT_KEY, TestDataProvider.PROJECT_NAME);
		return JsonUtil.toJson(node.transformToRest(ac, 0).toBlocking().value());
	}

}

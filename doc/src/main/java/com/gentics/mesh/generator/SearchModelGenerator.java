package com.gentics.mesh.generator;

import static com.gentics.mesh.dagger.SearchProviderType.TRACKING;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.mock.TestMocks.mockGroup;
import static com.gentics.mesh.mock.TestMocks.mockMicroschemaContainer;
import static com.gentics.mesh.mock.TestMocks.mockNode;
import static com.gentics.mesh.mock.TestMocks.mockNodeBasic;
import static com.gentics.mesh.mock.TestMocks.mockProject;
import static com.gentics.mesh.mock.TestMocks.mockRole;
import static com.gentics.mesh.mock.TestMocks.mockSchemaContainer;
import static com.gentics.mesh.mock.TestMocks.mockTag;
import static com.gentics.mesh.mock.TestMocks.mockTagFamily;
import static com.gentics.mesh.mock.TestMocks.mockUpdateDocumentEntry;
import static com.gentics.mesh.mock.TestMocks.mockUser;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.OrientDBContentDao;
import com.gentics.mesh.core.data.dao.OrientDBGroupDao;
import com.gentics.mesh.core.data.dao.OrientDBNodeDao;
import com.gentics.mesh.core.data.dao.OrientDBRoleDao;
import com.gentics.mesh.core.data.dao.OrientDBTagDao;
import com.gentics.mesh.core.data.dao.OrientDBTagFamilyDao;
import com.gentics.mesh.core.data.dao.OrientDBUserDao;
import com.gentics.mesh.core.data.dao.impl.ContentDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.GroupDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.NodeDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.RoleDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagFamilyDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.db.TxData;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.dagger.DaggerOrientDBMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

import io.vertx.core.json.JsonObject;

/**
 * Search document example JSON generator
 * 
 * This generator will create JSON files which represent the JSON documents that are stored within the elastic search index.
 */
public class SearchModelGenerator extends AbstractGenerator {

	public static File OUTPUT_ROOT_FOLDER = new File("src/main/docs/examples");

	private ObjectMapper mapper = new ObjectMapper();

	private TrackingSearchProviderImpl provider;

	private static MeshComponent meshDagger;

	public SearchModelGenerator(File outputDir) throws IOException {
		super(new File(outputDir, "search"));
	}

	/**
	 * Run the generator.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SearchModelGenerator searchModelGen = new SearchModelGenerator(OUTPUT_ROOT_FOLDER);
		searchModelGen.run();
	}

	/**
	 * Setup mesh to be used for search model generation.
	 * 
	 * @return
	 */
	public static Mesh initPaths() {
		OrientDBMeshOptions options = new OrientDBMeshOptions();
		options.setNodeName("Example Generator");
		options.getAuthenticationOptions().setKeystorePassword("ABCD");

		// Prefix all default directories in order to place them into the dump directory
		String uploads = "target/dump/" + options.getUploadOptions().getDirectory();
		new File(uploads).mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/dump/" + options.getUploadOptions().getTempDirectory();
		new File(targetTmpDir).mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/dump/" + options.getImageOptions().getImageCacheDirectory();
		new File(imageCacheDir).mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		options.getSearchOptions().setUrl(null);
		options.setNodeName("exampleGenerator");
		return Mesh.create(options);
	}

	/**
	 * Run the generator.
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		Mesh mesh = initPaths();
		// String baseDirProp = System.getProperty("baseDir");
		// if (baseDirProp == null) {
		// baseDirProp = "src" + File.separator + "main" + File.separator + "docs" + File.separator + "examples";
		// }
		// outputDir = new File(baseDirProp);
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");
		// outputDir.mkdirs();

		meshDagger = DaggerOrientDBMeshComponent.builder()
			.configuration(new OrientDBMeshOptions())
			.searchProviderType(TRACKING)
			.mesh(mesh)
			.build();
		provider = (TrackingSearchProviderImpl) meshDagger.searchProvider();

		try {
			Tx tx = mockTx();
			OrientDBNodeDao nodeDao = mock(NodeDaoWrapperImpl.class);
			OrientDBContentDao contentDao = mock(ContentDaoWrapperImpl.class);
			OrientDBUserDao userDao = mock(UserDaoWrapperImpl.class);
			OrientDBRoleDao roleDao = mock(RoleDaoWrapperImpl.class);
			OrientDBGroupDao groupDao = mock(GroupDaoWrapperImpl.class);
			OrientDBTagDao tagDao = mock(TagDaoWrapperImpl.class);
			OrientDBTagFamilyDao tagFamilyDao = mock(TagFamilyDaoWrapperImpl.class);

			when(tx.nodeDao()).thenReturn(nodeDao);
			when(tx.contentDao()).thenReturn(contentDao);
			when(tx.userDao()).thenReturn(userDao);
			when(tx.tagDao()).thenReturn(tagDao);
			when(tx.tagFamilyDao()).thenReturn(tagFamilyDao);
			when(tx.roleDao()).thenReturn(roleDao);
			when(tx.groupDao()).thenReturn(groupDao);

			writeNodeDocumentExample(nodeDao, contentDao, tagDao);
			writeTagDocumentExample();
			writeGroupDocumentExample();
			writeUserDocumentExample(userDao);
			writeRoleDocumentExample();
			writeProjectDocumentExample();
			writeTagFamilyDocumentExample(tagDao, tagFamilyDao);
			writeSchemaDocumentExample();
			writeMicroschemaDocumentExample();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
	}

	private Tx mockTx() {
		Tx txMock = mock(Tx.class);
		Tx.setActive(txMock);
		TxData txData = mock(TxData.class);
		when(txMock.data()).thenReturn(txData);
		return txMock;
	}

	private void writeNodeDocumentExample(OrientDBNodeDao nodeDao, OrientDBContentDao contentDao, OrientDBTagDao tagDao) throws Exception {
		String language = "de";
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibProject project = mockProject(user);
		HibTagFamily tagFamily = mockTagFamily("colors", user, project);
		HibTag tagA = mockTag("green", user, tagFamily, project);
		HibTag tagB = mockTag("red", user, tagFamily, project);
		HibNode parentNode = mockNodeBasic("folder", user);
		HibNode node = mockNode(nodeDao, contentDao, tagDao, parentNode, project, user, language, tagA, tagB);

		NodeIndexHandler nodeIndexHandler = meshDagger.nodeContainerIndexHandler();
		((NodeIndexHandlerImpl) nodeIndexHandler)
			.storeContainer(contentDao.getLatestDraftFieldContainer(node, language), UUID_1, ContainerType.PUBLISHED)
			.ignoreElement()
			.blockingAwait();
		writeStoreEvent("node.search");
	}

	private void writeProjectDocumentExample() throws Exception {
		HibUser creator = mockUser("admin", "Admin", "", null);
		HibUser user = mockUser("joe1", "Joe", "Doe", creator);
		HibProject project = mockProject(user);
		ProjectIndexHandler projectIndexHandler = meshDagger.projectIndexHandler();
		projectIndexHandler.store(project, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibGroup group = mockGroup("adminGroup", user);
		GroupIndexHandler groupIndexHandler = meshDagger.groupIndexHandler();
		groupIndexHandler.store(group, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibRole role = mockRole("adminRole", user);
		RoleIndexHandler roleIndexHandler = meshDagger.roleIndexHandler();
		roleIndexHandler.store(role, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("role.search");
	}

	private void writeUserDocumentExample(OrientDBUserDao userDao) throws Exception {
		HibUser creator = mockUser("admin", "Admin", "");
		HibUser user = mockUser("joe1", "Joe", "Doe", creator);
		HibGroup groupA = mockGroup("editors", user);
		HibGroup groupB = mockGroup("superEditors", user);
		Result<? extends HibGroup> result = new TraversalResult<>(Arrays.asList(groupA, groupB));
		when(userDao.getGroups(Mockito.any())).then(answer -> {
			return result;
		});
		UserIndexHandler userIndexHandler = meshDagger.userIndexHandler();
		userIndexHandler.store(user, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("user.search");
	}

	private void writeTagFamilyDocumentExample(OrientDBTagDao tagDao, OrientDBTagFamilyDao tagFamilyDao) throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibProject project = mockProject(user);
		HibTagFamily tagFamily = mockTagFamily("colors", user, project);
		List<HibTag> tagList = new ArrayList<>();
		tagList.add(mockTag("red", user, tagFamily, project));
		tagList.add(mockTag("green", user, tagFamily, project));

		when(tagDao.findAll(Mockito.any())).then(answer -> {
			return new TraversalResult<>(tagList);
		});
		when(tagFamilyDao.findAll(Mockito.any())).then(answer -> {
			return new TraversalResult<>(tagList);
		});

		TagFamilyIndexHandler tagFamilyIndexHandler = meshDagger.tagFamilyIndexHandler();
		UpdateDocumentEntry entry = mockUpdateDocumentEntry();

		tagFamilyIndexHandler.store(tagFamily, entry).blockingAwait();
		writeStoreEvent("tagFamily.search");
	}

	private void writeSchemaDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibSchema schemaContainer = mockSchemaContainer("content", user);

		SchemaIndexHandler searchIndexHandler = meshDagger.schemaContainerIndexHandler();
		searchIndexHandler.store(schemaContainer, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibMicroschema microschema = mockMicroschemaContainer("geolocation", user);

		MicroschemaIndexHandler searchIndexHandler = meshDagger.microschemaContainerIndexHandler();
		searchIndexHandler.store(microschema, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibProject project = mockProject(user);
		HibTagFamily tagFamily = mockTagFamily("colors", user, project);
		HibTag tag = mockTag("red", user, tagFamily, project);
		TagIndexHandler tagIndexHandler = meshDagger.tagIndexHandler();
		UpdateDocumentEntry entry = mockUpdateDocumentEntry();
		tagIndexHandler.store(tag, entry).blockingAwait();
		writeStoreEvent("tag.search");
	}

	private void writeStoreEvent(String name) throws Exception {
		JsonObject json = provider.getStoreEvents().values().iterator().next();
		if (json == null) {
			throw new RuntimeException("Could not find event to handle");
		}
		write(json, name);
		provider.reset();
	}

	private void write(JsonObject jsonObject, String filename) throws Exception {
		File file = new File(outputFolder, filename + ".json");
		System.out.println("Writing to {" + file.getAbsolutePath() + "}");
		JsonNode node = getMapper().readTree(jsonObject.toString());
		getMapper().writerWithDefaultPrettyPrinter().writeValue(file, node);
	}

	protected ObjectMapper getMapper() {
		return mapper;
	}
}

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
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.dagger.DaggerMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
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

	private TrackingSearchProvider provider;

	private static MeshComponent meshDagger;

	public SearchModelGenerator(File outputDir) throws IOException {
		super(new File(outputDir, "search"));
	}

	public static void main(String[] args) throws Exception {
		SearchModelGenerator searchModelGen = new SearchModelGenerator(OUTPUT_ROOT_FOLDER);
		searchModelGen.run();
	}

	public static Mesh initPaths() {
		MeshOptions options = new MeshOptions();
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

	public void run() throws Exception {
		Mesh mesh = initPaths();
		// String baseDirProp = System.getProperty("baseDir");
		// if (baseDirProp == null) {
		// baseDirProp = "src" + File.separator + "main" + File.separator + "docs" + File.separator + "examples";
		// }
		// outputDir = new File(baseDirProp);
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");
		// outputDir.mkdirs();

		meshDagger = DaggerMeshComponent.builder()
			.configuration(new MeshOptions())
			.searchProviderType(TRACKING)
			.mesh(mesh)
			.build();
		provider = (TrackingSearchProvider) meshDagger.searchProvider();

		try {
			TxData txData = mockTx();
			UserDaoWrapper userDao = mock(UserDaoWrapperImpl.class);
			TagDaoWrapper tagDao = mock(TagDaoWrapperImpl.class);
			when(txData.userDao()).thenReturn(userDao);
			when(txData.tagDao()).thenReturn(tagDao);

			writeNodeDocumentExample();
			writeTagDocumentExample();
			writeGroupDocumentExample();
			writeUserDocumentExample(userDao);
			writeRoleDocumentExample();
			writeProjectDocumentExample();
			writeTagFamilyDocumentExample(tagDao);
			writeSchemaDocumentExample();
			writeMicroschemaDocumentExample();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
	}

	private TxData mockTx() {
		Tx txMock = mock(Tx.class);
		Tx.setActive(txMock);
		TxData txData = mock(TxData.class);
		when(txMock.data()).thenReturn(txData);
		return txData;
	}

	private void writeNodeDocumentExample() throws Exception {
		String language = "de";
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tagA = mockTag("green", user, tagFamily, project);
		Tag tagB = mockTag("red", user, tagFamily, project);
		Node parentNode = mockNodeBasic("folder", user);
		Node node = mockNode(parentNode, project, user, language, tagA, tagB);

		NodeIndexHandlerImpl nodeIndexHandler = meshDagger.nodeContainerIndexHandler();
		nodeIndexHandler.storeContainer(node.getLatestDraftFieldContainer(language), UUID_1, ContainerType.PUBLISHED).toCompletable()
			.blockingAwait();
		writeStoreEvent("node.search");
	}

	private void writeProjectDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "", null);
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Project project = mockProject(user);
		ProjectIndexHandler projectIndexHandler = meshDagger.projectIndexHandler();
		projectIndexHandler.store(project, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		HibGroup group = mockGroup("adminGroup", user);
		GroupIndexHandler groupIndexHandler = meshDagger.groupIndexHandler();
		groupIndexHandler.store(group, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Role role = mockRole("adminRole", user);
		RoleIndexHandler roleIndexHandler = meshDagger.roleIndexHandler();
		roleIndexHandler.store(role, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("role.search");
	}

	private void writeUserDocumentExample(UserDaoWrapper userDao) throws Exception {
		User creator = mockUser("admin", "Admin", "");
		User user = mockUser("joe1", "Joe", "Doe", creator);
		HibGroup groupA = mockGroup("editors", user);
		HibGroup groupB = mockGroup("superEditors", user);
		TraversalResult<? extends HibGroup> result = new TraversalResult<>(Arrays.asList(groupA, groupB));
		when(userDao.getGroups(Mockito.any())).thenCallRealMethod();
		Mockito.<TraversalResult<? extends HibGroup>>when(user.getGroups()).thenReturn(result);
		UserIndexHandler userIndexHandler = meshDagger.userIndexHandler();
		userIndexHandler.store(user, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("user.search");
	}

	private void writeTagFamilyDocumentExample(TagDaoWrapper tagDao) throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		List<Tag> tagList = new ArrayList<>();
		tagList.add(mockTag("red", user, tagFamily, project));
		tagList.add(mockTag("green", user, tagFamily, project));

		when(tagDao.findAll(Mockito.any())).thenCallRealMethod();
		when(tagFamily.findAll()).then(answer -> {
			return new TraversalResult<>(tagList);
		});

		TagFamilyIndexHandler tagFamilyIndexHandler = meshDagger.tagFamilyIndexHandler();
		UpdateDocumentEntry entry = mockUpdateDocumentEntry();

		tagFamilyIndexHandler.store(tagFamily, entry).blockingAwait();
		writeStoreEvent("tagFamily.search");
	}

	private void writeSchemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Schema schemaContainer = mockSchemaContainer("content", user);

		SchemaContainerIndexHandler searchIndexHandler = meshDagger.schemaContainerIndexHandler();
		searchIndexHandler.store(schemaContainer, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Microschema microschema = mockMicroschemaContainer("geolocation", user);

		MicroschemaContainerIndexHandler searchIndexHandler = meshDagger.microschemaContainerIndexHandler();
		searchIndexHandler.store(microschema, mockUpdateDocumentEntry()).blockingAwait();
		writeStoreEvent("microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tag = mockTag("red", user, tagFamily, project);
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

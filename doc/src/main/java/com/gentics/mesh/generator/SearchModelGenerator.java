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
import static com.gentics.mesh.mock.TestMocks.mockUser;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.dao.impl.ContentDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.GroupDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.NodeDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.RoleDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagFamilyDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.DaggerOrientDBMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.microschema.MicroschemaTransformer;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.index.project.ProjectTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.schema.SchemaTransformer;
import com.gentics.mesh.search.index.tag.TagTransformer;
import com.gentics.mesh.search.index.tagfamily.TagFamilyTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Search document example JSON generator
 * 
 * This generator will create JSON files which represent the JSON documents that are stored within the elastic search index.
 */
public class SearchModelGenerator extends AbstractGenerator {

	public static File OUTPUT_ROOT_FOLDER = new File("src/main/docs/examples");

	private ObjectMapper mapper = new ObjectMapper();

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

		try {
			Tx tx = mockTx();
			NodeDaoWrapper nodeDao = mock(NodeDaoWrapperImpl.class);
			ContentDaoWrapper contentDao = mock(ContentDaoWrapperImpl.class);
			UserDaoWrapper userDao = mock(UserDaoWrapperImpl.class);
			RoleDaoWrapper roleDao = mock(RoleDaoWrapperImpl.class);
			GroupDaoWrapper groupDao = mock(GroupDaoWrapperImpl.class);
			TagDaoWrapper tagDao = mock(TagDaoWrapperImpl.class);
			TagFamilyDaoWrapper tagFamilyDao = mock(TagFamilyDaoWrapperImpl.class);

			when(tx.nodeDao()).thenReturn(nodeDao);
			when(tx.contentDao()).thenReturn(contentDao);
			when(tx.userDao()).thenReturn(userDao);
			when(tx.tagDao()).thenReturn(tagDao);
			when(tx.tagFamilyDao()).thenReturn(tagFamilyDao);
			when(tx.roleDao()).thenReturn(roleDao);
			when(tx.groupDao()).thenReturn(groupDao);

			writeNodeDocumentExample(nodeDao, contentDao, tagDao, roleDao);
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
		} finally {
			meshDagger.livenessManager().shutdown();
		}
	}

	private Tx mockTx() {
		Tx txMock = mock(Tx.class);
		Tx.setActive(txMock);
		TxData txData = mock(TxData.class);
		when(txMock.data()).thenReturn(txData);
		return txMock;
	}

	private void writeNodeDocumentExample(NodeDaoWrapper nodeDao, ContentDao contentDao, TagDaoWrapper tagDao, RoleDaoWrapper roleDao) throws Exception {
		String language = "de";
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibProject project = mockProject(user);
		HibTagFamily tagFamily = mockTagFamily("colors", user, project);
		HibTag tagA = mockTag("green", user, tagFamily, project);
		HibTag tagB = mockTag("red", user, tagFamily, project);
		HibNode parentNode = mockNodeBasic("folder", user);
		HibNode node = mockNode(nodeDao, contentDao, tagDao, parentNode, project, user, language, tagA, tagB);
		when(roleDao.getRolesWithPerm(Mockito.any(), Mockito.any())).thenReturn(new TraversalResult<>(Collections.emptyList()));

		write(new NodeContainerTransformer(new OrientDBMeshOptions(), roleDao).toDocument(contentDao.getLatestDraftFieldContainer(node, language), UUID_1, ContainerType.PUBLISHED), "node.search");
	}

	private void writeProjectDocumentExample() throws Exception {
		HibUser creator = mockUser("admin", "Admin", "", null);
		HibUser user = mockUser("joe1", "Joe", "Doe", creator);
		HibProject project = mockProject(user);
		write(new ProjectTransformer().toDocument(project), "project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibGroup group = mockGroup("adminGroup", user);
		write(new GroupTransformer().toDocument(group), "group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibRole role = mockRole("adminRole", user);
		write(new RoleTransformer().toDocument(role), "role.search");
	}

	private void writeUserDocumentExample(UserDaoWrapper userDao) throws Exception {
		HibUser creator = mockUser("admin", "Admin", "");
		HibUser user = mockUser("joe1", "Joe", "Doe", creator);
		HibGroup groupA = mockGroup("editors", user);
		HibGroup groupB = mockGroup("superEditors", user);
		Result<? extends HibGroup> result = new TraversalResult<>(Arrays.asList(groupA, groupB));
		when(userDao.getGroups(Mockito.any())).then(answer -> {
			return result;
		});
		write(new UserTransformer().toDocument(user), "user.search");
	}

	private void writeTagFamilyDocumentExample(TagDaoWrapper tagDao, TagFamilyDaoWrapper tagFamilyDao) throws Exception {
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

		write(new TagFamilyTransformer().toDocument(tagFamily), "tagFamily.search");
	}

	private void writeSchemaDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibSchema schemaContainer = mockSchemaContainer("content", user);

		write(new SchemaTransformer().toDocument(schemaContainer), "schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibMicroschema microschema = mockMicroschemaContainer("geolocation", user);

		write(new MicroschemaTransformer().toDocument(microschema), "microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		HibUser user = mockUser("joe1", "Joe", "Doe");
		HibProject project = mockProject(user);
		HibTagFamily tagFamily = mockTagFamily("colors", user, project);
		HibTag tag = mockTag("red", user, tagFamily, project);
		write(new TagTransformer().toDocument(tag), "tag.search");
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

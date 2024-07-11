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
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.DaggerHibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.data.dao.BranchDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.GroupDaoImpl;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.dao.RoleDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagFamilyDaoImpl;
import com.gentics.mesh.hibernate.data.dao.UserDaoImpl;
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
		HibernateMeshOptions options = new HibernateMeshOptions();
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

		meshDagger = DaggerHibernateMeshComponent.builder()
			.configuration(new HibernateMeshOptions())
			.searchProviderType(TRACKING)
			.mesh(mesh)
			.build();

		try {
			Tx tx = mockTx();
			NodeDao nodeDao = mock(NodeDaoImpl.class);
			BranchDao branchDao = mock(BranchDaoImpl.class);
			ContentDao contentDao = mock(ContentDaoImpl.class);
			UserDao userDao = mock(UserDaoImpl.class);
			RoleDao roleDao = mock(RoleDaoImpl.class);
			GroupDao groupDao = mock(GroupDaoImpl.class);
			TagDao tagDao = mock(TagDaoImpl.class);
			TagFamilyDao tagFamilyDao = mock(TagFamilyDaoImpl.class);

			when(tx.nodeDao()).thenReturn(nodeDao);
			when(tx.contentDao()).thenReturn(contentDao);
			when(tx.userDao()).thenReturn(userDao);
			when(tx.tagDao()).thenReturn(tagDao);
			when(tx.tagFamilyDao()).thenReturn(tagFamilyDao);
			when(tx.roleDao()).thenReturn(roleDao);
			when(tx.groupDao()).thenReturn(groupDao);
			when(tx.branchDao()).thenReturn(branchDao);

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

	private void writeNodeDocumentExample(NodeDao nodeDao, ContentDao contentDao, TagDao tagDao, RoleDao roleDao) throws Exception {
		String language = "de";
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tagA = mockTag("green", user, tagFamily, project);
		Tag tagB = mockTag("red", user, tagFamily, project);
		Node parentNode = mockNodeBasic("folder", user);
		Node node = mockNode(nodeDao, contentDao, tagDao, parentNode, project, user, language, tagA, tagB);
		when(roleDao.getRolesWithPerm(Mockito.any(), Mockito.any())).thenReturn(new TraversalResult<>(Collections.emptyList()));

		write(new NodeContainerTransformer(new HibernateMeshOptions(), roleDao).toDocument(contentDao.getLatestDraftFieldContainer(node, language), UUID_1, ContainerType.PUBLISHED), "node.search");
	}

	private void writeProjectDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "", null);
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Project project = mockProject(user);
		write(new ProjectTransformer().toDocument(project), "project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Group group = mockGroup("adminGroup", user);
		write(new GroupTransformer().toDocument(group), "group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Role role = mockRole("adminRole", user);
		write(new RoleTransformer().toDocument(role), "role.search");
	}

	private void writeUserDocumentExample(UserDao userDao) throws Exception {
		User creator = mockUser("admin", "Admin", "");
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Group groupA = mockGroup("editors", user);
		Group groupB = mockGroup("superEditors", user);
		Result<? extends Group> result = new TraversalResult<>(Arrays.asList(groupA, groupB));
		when(userDao.getGroups(Mockito.any())).then(answer -> {
			return result;
		});
		write(new UserTransformer().toDocument(user), "user.search");
	}

	private void writeTagFamilyDocumentExample(TagDao tagDao, TagFamilyDao tagFamilyDao) throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		List<Tag> tagList = new ArrayList<>();
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
		User user = mockUser("joe1", "Joe", "Doe");
		Schema schemaContainer = mockSchemaContainer("content", user);

		write(new SchemaTransformer().toDocument(schemaContainer), "schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Microschema microschema = mockMicroschemaContainer("geolocation", user);

		write(new MicroschemaTransformer().toDocument(microschema), "microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tag = mockTag("red", user, tagFamily, project);
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

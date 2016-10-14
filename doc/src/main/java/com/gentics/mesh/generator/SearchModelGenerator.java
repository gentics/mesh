package com.gentics.mesh.generator;

import static com.gentics.mesh.mock.Mocks.mockGroup;
import static com.gentics.mesh.mock.Mocks.mockLanguage;
import static com.gentics.mesh.mock.Mocks.mockMicroschemaContainer;
import static com.gentics.mesh.mock.Mocks.mockNode;
import static com.gentics.mesh.mock.Mocks.mockNodeBasic;
import static com.gentics.mesh.mock.Mocks.mockProject;
import static com.gentics.mesh.mock.Mocks.mockRole;
import static com.gentics.mesh.mock.Mocks.mockSchemaContainer;
import static com.gentics.mesh.mock.Mocks.mockTag;
import static com.gentics.mesh.mock.Mocks.mockTagFamily;
import static com.gentics.mesh.mock.Mocks.mockUser;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonObject;

/**
 * Search document example JSON generator
 * 
 * This generator will create JSON files which represent the JSON documents that are stored within the elastic search index.
 */
public class SearchModelGenerator {

	private File outputDir;

	private ObjectMapper mapper = new ObjectMapper();

	private DummySearchProvider provider;

	private static MeshComponent meshDagger;

	public static void main(String[] args) throws Exception {
		new SearchModelGenerator().start();
	}

	public static void initPaths() {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

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
		options.getSearchOptions().setDirectory(null);
		Mesh.mesh(options);
	}

	private void start() throws Exception {
		initPaths();
		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "src" + File.separator + "main" + File.separator + "docs" + File.separator + "json";
		}
		outputDir = new File(baseDirProp);
		System.out.println("Writing files to  {" + outputDir.getAbsolutePath() + "}");
		outputDir.mkdirs();

		meshDagger = MeshInternal.create();
		provider = (DummySearchProvider) meshDagger.searchProvider();

		try {
			writeNodeDocumentExample();
			writeTagDocumentExample();
			writeGroupDocumentExample();
			writeUserDocumentExample();
			writeRoleDocumentExample();
			writeProjectDocumentExample();
			writeTagFamilyDocumentExample();
			writeSchemaDocumentExample();
			writeMicroschemaDocumentExample();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
		System.exit(0);
	}

	private void writeNodeDocumentExample() throws Exception {
		Language language = mockLanguage("de");
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tagA = mockTag("green", user, tagFamily, project);
		Tag tagB = mockTag("red", user, tagFamily, project);
		Node parentNode = mockNodeBasic("folder", user);
		Node node = mockNode(parentNode, project, user, language, tagA, tagB);

		NodeIndexHandler nodeIndexHandler = meshDagger.nodeIndexHandler();
		nodeIndexHandler.storeContainer(node.getLatestDraftFieldContainer(language), null, null).await();
		writeStoreEvent("node.search");

	}

	private void writeProjectDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "", null);
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Project project = mockProject(user);
		ProjectIndexHandler projectIndexHandler = meshDagger.projectIndexHandler();
		projectIndexHandler.store(project, "project", null).await();
		writeStoreEvent("project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Group group = mockGroup("adminGroup", user);
		GroupIndexHandler groupIndexHandler = meshDagger.groupIndexHandler();
		groupIndexHandler.store(group, "group", null).await();
		writeStoreEvent("group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Role role = mockRole("adminRole", user);
		RoleIndexHandler roleIndexHandler = meshDagger.roleIndexHandler();
		roleIndexHandler.store(role, "role", null).await();
		writeStoreEvent("role.search");
	}

	private void writeUserDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "");
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Group groupA = mockGroup("editors", user);
		Group groupB = mockGroup("superEditors", user);
		Mockito.<List<? extends Group>> when(user.getGroups()).thenReturn(Arrays.asList(groupA, groupB));
		UserIndexHandler userIndexHandler = meshDagger.userIndexHandler();
		userIndexHandler.store(user, "user", null).await();
		writeStoreEvent("user.search");
	}

	private void writeTagFamilyDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		List<Tag> tagList = new ArrayList<>();
		tagList.add(mockTag("red", user, tagFamily, project));
		tagList.add(mockTag("green", user, tagFamily, project));

		when(tagFamily.getTagRoot().findAll()).then(answer -> {
			return tagList;
		});
		TagFamilyIndexHandler tagFamilyIndexHandler = meshDagger.tagFamilyIndexHandler();
		SearchQueueEntry entry = mock(SearchQueueEntry.class);
		when(entry.get("projectUuid")).thenReturn(UUIDUtil.randomUUID());
		tagFamilyIndexHandler.store(tagFamily, "tagFamily", entry).await();
		writeStoreEvent("tagFamily.search");
	}

	private void writeSchemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		SchemaContainer schemaContainer = mockSchemaContainer("content", user);

		SchemaContainerIndexHandler searchIndexHandler = meshDagger.schemaContainerIndexHandler();
		searchIndexHandler.store(schemaContainer, "schema", null).await();
		writeStoreEvent("schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		MicroschemaContainer microschemaContainer = mockMicroschemaContainer("geolocation", user);

		MicroschemaContainerIndexHandler searchIndexHandler = meshDagger.microschemaContainerIndexHandler();
		searchIndexHandler.store(microschemaContainer, "schema", null).await();
		writeStoreEvent("microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tag = mockTag("red", user, tagFamily, project);
		TagIndexHandler tagIndexHandler = meshDagger.tagIndexHandler();
		SearchQueueEntry entry = mock(SearchQueueEntry.class);
		when(entry.get("projectUuid")).thenReturn(UUIDUtil.randomUUID());
		tagIndexHandler.store(tag, "tag", entry).await();
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
		File file = new File(outputDir, filename + ".json");
		System.out.println("Writing to {" + file.getAbsolutePath() + "}");
		JsonNode node = getMapper().readTree(jsonObject.toString());
		getMapper().writerWithDefaultPrettyPrinter().writeValue(file, node);
	}

	public File getOutputDir() {
		return outputDir;
	}

	protected ObjectMapper getMapper() {
		return mapper;
	}
}

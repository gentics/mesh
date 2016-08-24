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
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.JsonNode;
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

import io.vertx.core.json.JsonObject;

/**
 * Search document example JSON generator
 * 
 * This generator will create JSON files which represent the JSON documents that are stored within the elastic search index.
 */
public class SearchModelGenerator extends AbstractGenerator {

	private DummySearchProvider provider;

	private AnnotationConfigApplicationContext ctx;

	static {
		System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "nodb");
	}

	public static void main(String[] args) throws Exception {
		new SearchModelGenerator().start();
	}

	private void start() throws Exception {

		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "src" + File.separator + "main" + File.separator + "docs" + File.separator + "json";
		}
		outputDir = new File(baseDirProp);
		System.out.println("Writing files to  {" + outputDir.getAbsolutePath() + "}");
		outputDir.mkdirs();

//		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringNoDBConfiguration.class)) {
//			this.ctx = ctx;
//			ctx.start();
//			ctx.registerShutdownHook();
//			provider = ctx.getBean("dummySearchProvider", DummySearchProvider.class);

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
//		}

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

		NodeIndexHandler nodeIndexHandler = ctx.getBean(NodeIndexHandler.class);
		nodeIndexHandler.storeContainer(node.getLatestDraftFieldContainer(language), null, null);
		writeStoreEvent("node.search");

	}

	private void writeProjectDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "", null);
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Project project = mockProject(user);
		ProjectIndexHandler projectIndexHandler = ctx.getBean(ProjectIndexHandler.class);
		projectIndexHandler.store(project, "project", null).await();
		writeStoreEvent("project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Group group = mockGroup("adminGroup", user);
		GroupIndexHandler groupIndexHandler = ctx.getBean(GroupIndexHandler.class);
		groupIndexHandler.store(group, "group", null).await();
		writeStoreEvent("group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Role role = mockRole("adminRole", user);
		RoleIndexHandler roleIndexHandler = ctx.getBean(RoleIndexHandler.class);
		roleIndexHandler.store(role, "role", null).await();
		writeStoreEvent("role.search");
	}

	private void writeUserDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "");
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Group groupA = mockGroup("editors", user);
		Group groupB = mockGroup("superEditors", user);
		Mockito.<List<? extends Group>> when(user.getGroups()).thenReturn(Arrays.asList(groupA, groupB));
		UserIndexHandler userIndexHandler = ctx.getBean(UserIndexHandler.class);
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
		TagFamilyIndexHandler tagFamilyIndexHandler = ctx.getBean(TagFamilyIndexHandler.class);
		tagFamilyIndexHandler.store(tagFamily, "tagFamily", null).await();
		writeStoreEvent("tagFamily.search");
	}

	private void writeSchemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		SchemaContainer schemaContainer = mockSchemaContainer("content", user);

		SchemaContainerIndexHandler searchIndexHandler = ctx.getBean(SchemaContainerIndexHandler.class);
		searchIndexHandler.store(schemaContainer, "schema", null).await();
		writeStoreEvent("schema.search");
	}

	private void writeMicroschemaDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		MicroschemaContainer microschemaContainer = mockMicroschemaContainer("geolocation", user);

		MicroschemaContainerIndexHandler searchIndexHandler = ctx.getBean(MicroschemaContainerIndexHandler.class);
		searchIndexHandler.store(microschemaContainer, "schema", null).await();
		writeStoreEvent("microschema.search");
	}

	private void writeTagDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Project project = mockProject(user);
		TagFamily tagFamily = mockTagFamily("colors", user, project);
		Tag tag = mockTag("red", user, tagFamily, project);
		TagIndexHandler tagIndexHandler = ctx.getBean(TagIndexHandler.class);
		tagIndexHandler.store(tag, "tag", null).await();
		writeStoreEvent("tag.search");
	}

	private void writeStoreEvent(String name) throws Exception {
		JsonObject eventMap = provider.getStoreEvents().values().iterator().next();
		if (eventMap == null) {
			throw new RuntimeException("Could not find event to handle");
		}
		Map<String, Object> outputMap = new TreeMap<>();
		// System.out.println(new JSONObject(eventMap).toString(4));
		//TODO flatten json?
//		flatten(eventMap, outputMap, null);
		JSONObject json = new JSONObject(outputMap);
		write(json, name);
		provider.reset();
	}

	private void write(JSONObject jsonObject, String filename) throws Exception {
		File file = new File(outputDir, filename + ".json");
		System.out.println("Writing to {" + file.getAbsolutePath() + "}");
		JsonNode node = getMapper().readTree(jsonObject.toString());
		getMapper().writerWithDefaultPrettyPrinter().writeValue(file, node);
	}

	private void flatten(Map<String, Object> map, Map<String, Object> output, String key) throws JSONException {
		String prefix = "";
		if (key != null) {
			prefix = key + ".";
		}
		for (Entry<String, Object> entry : map.entrySet()) {
			String currentKey = prefix + entry.getKey();
			if (entry.getValue() instanceof Map) {
				flatten((Map<String, Object>) entry.getValue(), output, prefix + entry.getKey());
			} else if (entry.getValue() instanceof List) {
				output.put(currentKey, entry.getValue());
			} else {
				output.put(currentKey, entry.getValue());
			}
		}
	}
}

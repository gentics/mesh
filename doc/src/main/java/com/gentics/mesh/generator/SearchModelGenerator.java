package com.gentics.mesh.generator;

import static com.gentics.mesh.mock.MockingUtils.mockGroup;
import static com.gentics.mesh.mock.MockingUtils.mockLanguage;
import static com.gentics.mesh.mock.MockingUtils.mockNode;
import static com.gentics.mesh.mock.MockingUtils.mockNodeBasic;
import static com.gentics.mesh.mock.MockingUtils.mockProject;
import static com.gentics.mesh.mock.MockingUtils.mockRole;
import static com.gentics.mesh.mock.MockingUtils.mockTag;
import static com.gentics.mesh.mock.MockingUtils.mockTagFamily;
import static com.gentics.mesh.mock.MockingUtils.mockUser;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.search.index.GroupIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.RoleIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;
import com.gentics.mesh.search.index.UserIndexHandler;
import com.gentics.mesh.test.DummySearchProvider;

public class SearchModelGenerator extends AbstractGenerator {

	private DummySearchProvider provider;

	private AnnotationConfigApplicationContext ctx;

	public static void main(String[] args) throws Exception {
		new SearchModelGenerator().start();
	}

	private void start() throws Exception {

		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "target" + File.separator + "site" + File.separator + "docs" + File.separator + "search";
		}
		File baseDir = new File(baseDirProp);
		outputDir = new File(baseDir, "json");
		System.out.println("Writing files to  {" + outputDir.getAbsolutePath() + "}");
		outputDir.mkdirs();

		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringNoDBConfiguration.class)) {
			this.ctx = ctx;
			ctx.start();
			ctx.registerShutdownHook();
			provider = ctx.getBean("dummySearchProvider", DummySearchProvider.class);

			writeNodeDocumentExample();
			writeTagDocumentExample();
			writeGroupDocumentExample();
			writeUserDocumentExample();
			writeRoleDocumentExample();
			writeProjectDocumentExample();
			writeTagFamilyDocumentExample();
			System.exit(0);
		}

	}

	private void writeNodeDocumentExample() throws Exception {
		Language language = mockLanguage("de");
		User user = mockUser("joe1", "Joe", "Doe");
		TagFamily tagFamily = mockTagFamily("colors", user);
		Tag tagA = mockTag("green", user, tagFamily);
		Tag tagB = mockTag("red", user, tagFamily);
		Project project = mockProject(user);
		Node parentNode = mockNodeBasic("folder");
		Node node = mockNode(parentNode, project, user, language, tagA, tagB);
		NodeIndexHandler nodeIndexHandler = ctx.getBean(NodeIndexHandler.class);
		nodeIndexHandler.store(node, "node", rh -> {
		});
		writeStoreEvent("node.search");

	}

	private void writeProjectDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "", null);

		User user = mockUser("joe1", "Joe", "Doe", creator);
		Project project = mockProject(user);
		ProjectIndexHandler projectIndexHandler = ctx.getBean(ProjectIndexHandler.class);
		projectIndexHandler.store(project, "project", rh -> {
		});
		writeStoreEvent("project.search");
	}

	private void writeGroupDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Group group = mockGroup("adminGroup", user);
		GroupIndexHandler groupIndexHandler = ctx.getBean(GroupIndexHandler.class);
		groupIndexHandler.store(group, "group", rh -> {
		});
		writeStoreEvent("group.search");
	}

	private void writeRoleDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		Role role = mockRole("adminRole", user);
		RoleIndexHandler roleIndexHandler = ctx.getBean(RoleIndexHandler.class);
		roleIndexHandler.store(role, "role", rh -> {
		});
		writeStoreEvent("role.search");
	}

	private void writeUserDocumentExample() throws Exception {
		User creator = mockUser("admin", "Admin", "");
		User user = mockUser("joe1", "Joe", "Doe", creator);
		Group groupA = mockGroup("editors", user);
		Group groupB = mockGroup("superEditors", user);
		Mockito.<List<? extends Group>> when(user.getGroups()).thenReturn(Arrays.asList(groupA, groupB));
		UserIndexHandler userIndexHandler = ctx.getBean(UserIndexHandler.class);
		userIndexHandler.store(user, "user", rh -> {
		});
		writeStoreEvent("user.search");

	}

	private void writeTagFamilyDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		TagFamily tagFamily = mockTagFamily("colors", user);
		TagFamilyIndexHandler tagFamilyIndexHandler = ctx.getBean(TagFamilyIndexHandler.class);
		tagFamilyIndexHandler.store(tagFamily, "tagFamily", rh -> {
		});
		writeStoreEvent("tagFamily.search");

	}

	private void writeTagDocumentExample() throws Exception {
		User user = mockUser("joe1", "Joe", "Doe");
		TagFamily tagFamily = mockTagFamily("colors", user);
		Tag tag = mockTag("red", user, tagFamily);
		TagIndexHandler tagIndexHandler = ctx.getBean(TagIndexHandler.class);
		tagIndexHandler.store(tag, "tag", rh -> {
		});
		writeStoreEvent("tag.search");

	}

	private void writeStoreEvent(String name) throws Exception {
		Map<String, Object> eventMap = provider.getStoreEvents().values().iterator().next();
		if (eventMap == null) {
			throw new RuntimeException("Could not find event to handle");
		}
		Map<String, Object> outputMap = new TreeMap<>();
		flatten(eventMap, outputMap, null);
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
			} else if (entry.getValue() instanceof String) {
				output.put(currentKey, entry.getValue());
			}
		}
	}

}

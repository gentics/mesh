package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.parameter.impl.SortingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.DateUtils;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SortingEndpointTest extends AbstractMeshTest {

	@Test
	public void testSortSchemas() {
		final Map<String, Function<SchemaResponse, String>> getters = Map.of(
				"name", SchemaResponse::getName,
				"uuid", SchemaResponse::getUuid,
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(),
				"creator.lastname", r -> r.getCreator().getLastName(),
				"editor.firstname", r -> r.getEditor().getFirstName(),
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName(name);
			call(() -> client().createSchema(request));
		}
		Consumer<String> test = field -> {
			Function<SchemaResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting schemas by " + field + " cannot currently be tested");
				return;
			}
			SchemaListResponse list = call(() -> client().findSchemas(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 13", 13, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.schemaDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortMicroschemas() {
		final Map<String, Function<MicroschemaResponse, String>> getters = Map.of(
				"name", MicroschemaResponse::getName, 
				"uuid", MicroschemaResponse::getUuid, 
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(), 
				"creator.lastname", r -> r.getCreator().getLastName(), 
				"editor.firstname", r -> r.getEditor().getFirstName(), 
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			MicroschemaCreateRequest request = new MicroschemaCreateRequest();
			request.setName(name);
			call(() -> client().createMicroschema(request));
		}
		Consumer<String> test = field -> {
			Function<MicroschemaResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting microschemas by " + field + " cannot currently be tested");
				return;
			}
			MicroschemaListResponse list = call(() -> client().findMicroschemas(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 12", 12, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.microschemaDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortUsers() throws Exception {
		final Map<String, Function<UserResponse, String>> getters = Map.ofEntries(
				Map.entry("username", UserResponse::getUsername), 
				Map.entry("firstname", UserResponse::getFirstname), 
				Map.entry("lastname", UserResponse::getLastname), 
				Map.entry("emailAddress", UserResponse::getEmailAddress), 
				Map.entry("forcedPasswordChange", r -> Boolean.toString(r.getForcedPasswordChange())), 
				Map.entry("uuid", UserResponse::getUuid), 
				Map.entry("created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false))),
				Map.entry("edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false))),
				Map.entry("creator.firstname", r -> r.getCreator().getFirstName()), 
				Map.entry("creator.lastname", r -> r.getCreator().getLastName()), 
				Map.entry("editor.firstname", r -> r.getEditor().getFirstName()), 
				Map.entry("editor.lastname", r -> r.getEditor().getLastName())
			);
		RandomStringUtils rnd = RandomStringUtils.insecure();
		for (int i = 1; i < 11; i++) {
			final String name = "sort12345_" + i;
			UserCreateRequest request = new UserCreateRequest();
			request.setFirstname(rnd.nextAlphanumeric(i));
			request.setLastname(rnd.nextAlphanumeric(i));
			request.setEmailAddress(rnd.nextAlphanumeric(i) + "@" + rnd.nextAlphanumeric(i));
			request.setUsername(name);
			request.setPassword(name);
			request.setForcedPasswordChange((i % 2) > 0);
			call(() -> client().createUser(request));
		}
		Consumer<String> test = field -> {
			Function<UserResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting users by " + field + " cannot currently be tested");
				return;
			}
			UserListResponse list = call(() -> client().findUsers(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 14", 14, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.userDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortTagFamilies() {
		final Map<String, Function<TagFamilyResponse, String>> getters = Map.of(
				"name", TagFamilyResponse::getName, 
				"uuid", TagFamilyResponse::getUuid, 
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(), 
				"creator.lastname", r -> r.getCreator().getLastName(), 
				"editor.firstname", r -> r.getEditor().getFirstName(), 
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			TagFamilyCreateRequest request = new TagFamilyCreateRequest();
			request.setName(name);
			call(() -> client().createTagFamily(projectName(), request));
		}
		Consumer<String> test = field -> {
			Function<TagFamilyResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting tag families by " + field + " cannot currently be tested");
				return;
			}
			TagFamilyListResponse list = call(() -> client().findTagFamilies(projectName(), new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 12", 12, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.tagFamilyDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortTags() {
		final Map<String, Function<TagResponse, String>> getters = Map.of(
				"name", TagResponse::getName, 
				"uuid", TagResponse::getUuid, 
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(), 
				"creator.lastname", r -> r.getCreator().getLastName(), 
				"editor.firstname", r -> r.getEditor().getFirstName(), 
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		String tagFamilyUuid = tx(() -> tagFamily("basic").getUuid());
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			TagCreateRequest request = new TagCreateRequest();
			request.setName(name);
			call(() -> client().createTag(projectName(), tagFamilyUuid, request));
		}
		Consumer<String> test = field -> {
			Function<TagResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting tags by " + field + " cannot currently be tested");
				return;
			}
			TagListResponse list = call(() -> client().findTags(projectName(), tagFamilyUuid, new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 19", 19, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.tagDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortRoles() {
		final Map<String, Function<RoleResponse, String>> getters = Map.of(
				"name", RoleResponse::getName,
				"uuid", RoleResponse::getUuid,
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(),
				"creator.lastname", r -> r.getCreator().getLastName(),
				"editor.firstname", r -> r.getEditor().getFirstName(),
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName(name);
			call(() -> client().createRole(request));
		}
		Consumer<String> test = field -> {
			Function<RoleResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting roles by " + field + " cannot currently be tested");
				return;
			}
			RoleListResponse list = call(() -> client().findRoles(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 15", 15, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.roleDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortProjects() {
		final Map<String, Function<ProjectResponse, String>> getters = Map.of(
				"name", ProjectResponse::getName,
				"uuid", ProjectResponse::getUuid,
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(),
				"creator.lastname", r -> r.getCreator().getLastName(),
				"editor.firstname", r -> r.getEditor().getFirstName(),
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(request));
		}
		Consumer<String> test = field -> {
			Function<ProjectResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting projects by " + field + " cannot currently be tested");
				return;
			}
			ProjectListResponse list = call(() -> client().findProjects(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 11", 11, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.projectDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortGroups() {
		final Map<String, Function<GroupResponse, String>> getters = Map.of(
				"name", GroupResponse::getName,
				"uuid", GroupResponse::getUuid,
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(),
				"creator.lastname", r -> r.getCreator().getLastName(),
				"editor.firstname", r -> r.getEditor().getFirstName(),
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			GroupCreateRequest request = new GroupCreateRequest();
			request.setName(name);
			call(() -> client().createGroup(request));
		}
		Consumer<String> test = field -> {
			Function<GroupResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting groups by " + field + " cannot currently be tested");
				return;
			}
			GroupListResponse list = call(() -> client().findGroups(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 15", 15, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.groupDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortBranches() {
		final Map<String, Function<BranchResponse, String>> getters = Map.of(
				"name", BranchResponse::getName,
				"uuid", BranchResponse::getUuid,
				"created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false)),
				"edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false)),
				"creator.firstname", r -> r.getCreator().getFirstName(),
				"creator.lastname", r -> r.getCreator().getLastName(),
				"editor.firstname", r -> r.getEditor().getFirstName(),
				"editor.lastname", r -> r.getEditor().getLastName()
			);
		for (int i = 0; i < 10; i++) {
			final String name = "sort12345_" + i;
			BranchCreateRequest request = new BranchCreateRequest();
			request.setName(name);
			call(() -> client().createBranch(projectName(), request));
		}
		Consumer<String> test = field -> {
			Function<BranchResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting branches by " + field + " cannot currently be tested");
				return;
			}
			BranchListResponse list = call(() -> client().findBranches(projectName(), new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 11", 11, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.branchDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortLanguages() {
		final Map<String, Function<LanguageResponse, String>> getters = Map.of(
				"uuid", AbstractResponse::getUuid, 
				"name", LanguageResponse::getName, 
				// Fails on checking the non latin characters against Java comparator
				//"nativeName", LanguageResponse::getNativeName, 
				"languageTag", LanguageResponse::getLanguageTag
			);
		Consumer<String> test = field -> {
			Function<LanguageResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting languages by " + field + " cannot currently be tested");
				return;
			}
			LanguageListResponse list = call(() -> client().findLanguages(new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 11", 11, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field)
				.withFailMessage("Languages are not sorted by " + field + ":\n%s", list.getData().stream().map(r -> getters.get(field).apply(r)).collect(Collectors.joining(",\n", "<", ">")))
				.isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null));
		};		
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.languageDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			test.accept(field);
		}
	}

	@Test
	public void testSortNodes() {
		Function<StringField, String> safeGet = field -> field != null ? field.getString() : null;
		final Map<String, Function<NodeResponse, String>> getters = Map.ofEntries(
				Map.entry("uuid", NodeResponse::getUuid),
				Map.entry("created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false))),
				Map.entry("edited", r -> Long.toString(DateUtils.fromISO8601(r.getEdited(), false))),
				Map.entry("creator.firstname", r -> r.getCreator().getFirstName()),
				Map.entry("creator.lastname", r -> r.getCreator().getLastName()),
				Map.entry("editor.firstname", r -> r.getEditor().getFirstName()),
				Map.entry("editor.lastname", r -> r.getEditor().getLastName()),
				Map.entry("schema.name", r -> r.getSchema().getName()),
				Map.entry("schema.uuid", r -> r.getSchema().getUuid()),
				Map.entry("fields.content.title", r -> safeGet.apply(r.getFields().getStringField("title"))),
				Map.entry("fields.content.teaser", r -> safeGet.apply(r.getFields().getStringField("teaser"))),
				Map.entry("fields.content.slug", r -> safeGet.apply(r.getFields().getStringField("slug"))),
				Map.entry("fields.content.content", r -> safeGet.apply(r.getFields().getStringField("content")))
			);
		HibNode parentNode = folder("news");
		String uuid = tx(() -> parentNode.getUuid());
		HibSchema schema = schemaContainer("content");
		String schemaUuid = tx(() -> schema.getUuid());
		RandomStringUtils rnd = RandomStringUtils.insecure();
		for (int i = 0; i < 100; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(schemaUuid));
			request.setLanguage("en");
			request.getFields().put("title", createStringField("some title " + i));
			request.getFields().put("teaser", createStringField("some teaser " + rnd.nextAlphanumeric(i+1)));
			request.getFields().put("slug", createStringField("newpage" + i + ".html"));
			// fails on checking French characters in Concorde definition, which does not match Java comparison
			//request.getFields().put("content", createStringField(rnd.nextAlphanumeric(i+1)));
			request.setParentNodeUuid(uuid);
			call(() -> client().createNode(PROJECT_NAME, request));			
		}
		Consumer<String> test = field -> {
			Function<NodeResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting nodes by " + field + " cannot currently be tested");
				return;
			}
			NodeListResponse list = call(() -> client().findNodes(projectName(), new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 116", 116, list.getData().size());
			assertThat(list.getData()).as("Sorted by " + field)
				.withFailMessage("Nodes are not sorted by " + field + ":\n%s", list.getData().stream().map(r -> getters.get(field).apply(r)).collect(Collectors.joining(",\n", "<", ">")))
				.isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
					fa != null ? context.apply(fa) : null,
					fb != null ? context.apply(fb) : null)
				);
		};		
		Consumer<String> testFields = field -> {
			Function<NodeResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting nodes by " + field + " cannot currently be tested");
				return;
			}
			NodeListResponse list = call(() -> client().findNodes(projectName(), new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 116", 116, list.getData().size());
			List<NodeResponse> listData = list.getData().stream().filter(item -> "content".equals(item.getSchema().getName())).collect(Collectors.toList());
			assertEquals("Total requested schema's data size should be 109", 109, listData.size());
			Map<String, List<NodeResponse>> byVersion = listData.stream().collect(Collectors.groupingBy(item -> item.getVersion()));
			for (Entry<String, List<NodeResponse>> version : byVersion.entrySet()) {
				assertThat(version.getValue()).as("Version " + version.getKey() + " sorted by " + field)
					.withFailMessage("Nodes are not sorted by " + field + ":\n%s", version.getValue().stream().map(r -> getters.get(field).apply(r)).collect(Collectors.joining(",\n", "<", ">")))
					.isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
						fa != null ? context.apply(fa) : null,
						fb != null ? context.apply(fb) : null)
					);
			}
		};	
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.nodeDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			if (field.startsWith("fields.<")) {
				continue;
			}
			test.accept(field);
		}
		for (FieldSchema field : tx(() -> schema.getLatestVersion().getSchema().getFields())) {
			testFields.accept("fields.content." + field.getName());
		}
	}
}

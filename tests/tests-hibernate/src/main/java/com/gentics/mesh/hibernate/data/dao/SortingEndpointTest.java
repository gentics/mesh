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
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.SortingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.schemaDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Schemas", field, getters, sortOrder -> client().findSchemas(new SortingParametersImpl(field, sortOrder)), 13);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.microschemaDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Microschemas", field, getters, sortOrder -> client().findMicroschemas(new SortingParametersImpl(field, sortOrder)), 12);
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
			request.setFirstname("fn" + rnd.nextAlphanumeric(i));
			request.setLastname("ln" + rnd.nextAlphanumeric(i));
			request.setEmailAddress("mail" + rnd.nextAlphanumeric(i) + "@" + rnd.nextAlphanumeric(i) + "domain");
			request.setUsername(name);
			request.setPassword(name);
			request.setForcedPasswordChange((i % 2) > 0);
			call(() -> client().createUser(request));
		}	
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.userDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Users", field, getters, sortOrder -> client().findUsers(new SortingParametersImpl(field, sortOrder)), 14);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.tagFamilyDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Tag Families", field, getters, sortOrder -> client().findTagFamilies(projectName(), new SortingParametersImpl(field, sortOrder)), 12);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.tagDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Tags", field, getters, sortOrder -> client().findTags(projectName(), tagFamilyUuid, new SortingParametersImpl(field, sortOrder)), 19);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.roleDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Roles", field, getters, sortOrder -> client().findRoles(new SortingParametersImpl(field, sortOrder)), 15);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.projectDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Projects", field, getters, sortOrder -> client().findProjects(new SortingParametersImpl(field, sortOrder)), 11);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.groupDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Groups", field, getters, sortOrder -> client().findGroups(new SortingParametersImpl(field, sortOrder)), 15);
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
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.branchDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Branches", field, getters, sortOrder -> client().findBranches(projectName(), new SortingParametersImpl(field, sortOrder)), 11);
		}
	}

	@Test
	public void testSortLanguages() {
		final Map<String, Function<LanguageResponse, String>> getters = Map.of(
				"uuid", AbstractResponse::getUuid, 
				"name", LanguageResponse::getName, 
				// Fails on checking non latin characters against Java comparator
				//"nativeName", LanguageResponse::getNativeName, 
				"languageTag", LanguageResponse::getLanguageTag
			);	
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.languageDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Languages", field, getters, sortOrder -> client().findLanguages(new SortingParametersImpl(field, sortOrder)), 11);
		}
	}

	@Test
	public void testSortJobs() {
		JobListResponse jobList = adminCall(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		waitForJob(() -> {
			SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
			schema.setName("folder2");
			call(() -> client().updateSchema(uuid, schema));
		});

		tx((tx) -> {
			tx.jobDao().enqueueBranchMigration(user(), initialBranch());
		});
		final Map<String, Function<JobResponse, String>> getters = Map.ofEntries(
				Map.entry("uuid", JobResponse::getUuid),
				Map.entry("created", r -> Long.toString(DateUtils.fromISO8601(r.getCreated(), false))),
				Map.entry("creator.firstname", r -> r.getCreator().getFirstName()),
				Map.entry("creator.lastname", r -> r.getCreator().getLastName()),
				Map.entry("type", r -> r.getType().toString()), 
				Map.entry("status", r -> r.getStatus().toString()), 
				Map.entry("stopDate", r -> r.getStopDate()),
				Map.entry("startDate", r -> r.getStartDate()),
				Map.entry("nodeName", r -> r.getNodeName())
			);	
		String[] fields = tx(tx -> { 
			return ((HibQueryFieldMapper) tx.languageDao()).getGraphQlSortingFieldNames(false);
		});
		for (String field : fields) {
			sortBackAndForth("Jobs", field, getters, sortOrder -> client().findJobs(new SortingParametersImpl(field, sortOrder)), 2);
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
				// fails on checking French characters in Concorde definition, which does not match Java comparison rules
				//Map.entry("fields.content.content", r -> safeGet.apply(r.getFields().getStringField("content"))),
				Map.entry("fields.content.title", r -> safeGet.apply(r.getFields().getStringField("title"))),
				Map.entry("fields.content.teaser", r -> safeGet.apply(r.getFields().getStringField("teaser"))),
				Map.entry("fields.content.slug", r -> safeGet.apply(r.getFields().getStringField("slug")))
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
			request.getFields().put("content", createStringField("HTML " + rnd.nextAlphanumeric(i+1)));
			request.setParentNodeUuid(uuid);
			call(() -> client().createNode(PROJECT_NAME, request));			
		}		
		Consumer<String> testFields = field -> {
			Function<NodeResponse, String> context = getters.get(field);
			if (context == null) {
				System.out.println("Sorting nodes , by `" + field + "` cannot currently be tested");
				return;
			}
			NodeListResponse list = call(() -> client().findNodes(projectName(), new SortingParametersImpl(field, SortOrder.DESCENDING)));
			assertEquals("Total data size should be 116", 116, list.getData().size());
			List<NodeResponse> listData = list.getData().stream().filter(item -> "content".equals(item.getSchema().getName())).collect(Collectors.toList());
			assertEquals("Total requested schema's data size should be 109", 109, listData.size());

			// Since sorting is performed against schema versioned content, it is grouped by version in the response, but not sorted in general
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
			sortBackAndForth("Nodes", field, getters, sortOrder -> client().findNodes(projectName(), new SortingParametersImpl(field, sortOrder)), 116);
		}
		for (FieldSchema field : tx(() -> schema.getLatestVersion().getSchema().getFields())) {
			testFields.accept("fields.content." + field.getName());
		}
	}

	protected <M extends RestModel> void sortBackAndForth(String name, String field, Map<String, Function<M, String>> getters, Function<SortOrder, MeshRequest<? extends ListResponse<M>>> request, long totalSize) {
		Function<M, String> context = getters.get(field);
		if (context == null) {
			System.out.println("Sorting " + name + "  by `" + field + "` cannot currently be tested");
			return;
		}
		ListResponse<M> list = adminCall(() -> request.apply(SortOrder.ASCENDING));
		assertEquals(name + " total data size should be " + totalSize, totalSize, list.getData().size());
		assertThat(list.getData()).as("Sorted by " + field)
			.withFailMessage(name + " are not sorted by " + field + ":\n%s", list.getData().stream().map(r -> getters.get(field).apply(r)).collect(Collectors.joining(",\n", "<", ">")))
			.isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().compare(
				fa != null ? context.apply(fa) : null,
				fb != null ? context.apply(fb) : null)
			);

		list = adminCall(() -> request.apply(SortOrder.DESCENDING));
		assertEquals(name + " total data size should be " + totalSize, totalSize, list.getData().size());
		assertThat(list.getData()).as("Sorted by " + field)
			.withFailMessage(name + " are not sorted by " + field + ":\n%s", list.getData().stream().map(r -> getters.get(field).apply(r)).collect(Collectors.joining(",\n", "<", ">")))
			.isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
				fa != null ? context.apply(fa) : null,
				fb != null ? context.apply(fb) : null)
			);
	};
}

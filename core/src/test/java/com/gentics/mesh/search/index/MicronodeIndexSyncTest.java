package com.gentics.mesh.search.index;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.helper.ExpectedEvent;
import com.gentics.mesh.test.context.helper.UnexpectedEvent;
import com.gentics.mesh.util.Tuple;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for elasticsearch indices for schemas using microschemas
 */
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class MicronodeIndexSyncTest extends AbstractMeshTest {
	protected final static String PROJECT_NAME = "testproject";

	protected final static String SCHEMA_NAME = "schema";

	protected final static String MICROSCHEMA_NAME = "micro";

	protected final static String MICRONODE_FIELD1_NAME = "microfield1";

	protected final static String MICRONODE_FIELD2_NAME = "microfield2";

	protected final static String MICROSCHEMA_FIELD_NAME = "one";

	private ProjectResponse project;

	private String branchUuid;

	/**
	 * Setup test data:
	 * <ol>
	 * <li>Create a project</li>
	 * <li>Create microschema</li>
	 * <li>Create schema using the microschema</li>
	 * <li>Assign schema and microschema to the project</li>
	 * </ol>
	 * @throws Exception
	 */
	@Before
	public void setupTestData() throws Exception {
		project = createProject(PROJECT_NAME);

		BranchListResponse branches = call(() -> client().findBranches(PROJECT_NAME));
		branchUuid = branches.getData().stream().filter(BranchResponse::getLatest).map(BranchResponse::getUuid)
				.findFirst().orElseThrow(() -> new Exception("Did not find default branch"));
	}

	/**
	 * Test that the indices were created with the expected names (containing hash over the microschema version uuids)
	 * @throws Exception
	 */
	@Test
	public void testIndexCreation() throws Exception {
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});
		createSchema(SCHEMA_NAME, req -> {
			req.addField(
					FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
			req.addField(FieldUtil.createListFieldSchema(MICRONODE_FIELD2_NAME, "micronode")
					.setAllowedSchemas(MICROSCHEMA_NAME));
		});
		waitForSearchIdleEvent();
		refreshIndices();
		new TestSchemaInfo(SCHEMA_NAME).assertIndices();
	}

	/**
	 * Test that updating the microschema creates new indices and migrates all data
	 * @throws Exception
	 */
	@Test
	public void testUpdateMicroschema() throws Exception {
		MicroschemaResponse microschema = createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});
		createSchema(SCHEMA_NAME, req -> {
			req.addField(
					FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
			req.addField(FieldUtil.createListFieldSchema(MICRONODE_FIELD2_NAME, "micronode")
					.setAllowedSchemas(MICROSCHEMA_NAME));
		});

		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");

			// every 2nd node will contain a micronode
			if (i % 2 == 0) {
				request.getFields().put(MICRONODE_FIELD1_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
						Tuple.tuple(MICROSCHEMA_FIELD_NAME, FieldUtil.createStringField("bla"))));
			}

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			// every 3rd node will be published
			if (i % 3 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}

		waitForSearchIdleEvent();
		refreshIndices();

		TestSchemaInfo schemaInfo = new TestSchemaInfo(SCHEMA_NAME).assertIndices()
				.assertIndexedDraftDocuments(draftNodeUuids).assertIndexedPublishedDocuments(publishedNodeUuids);

		String oldDraftIndex = schemaInfo.expectedDraftIndex;
		String oldPublishedIndex = schemaInfo.expectedPublishedIndex;
		updateMicroschema(microschema.getUuid(), update -> {
			update.addField(new StringFieldSchemaImpl().setName("two"));
		});

		waitForSearchIdleEvent();
		refreshIndices();

		schemaInfo = new TestSchemaInfo(SCHEMA_NAME);
		assertThat(schemaInfo.expectedDraftIndex).as("draft index name").isNotEqualTo(oldDraftIndex);
		assertThat(schemaInfo.expectedPublishedIndex).as("published index name").isNotEqualTo(oldPublishedIndex);

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);
	}

	/**
	 * Test that the index check will accept the indices
	 * @throws Exception
	 */
	@Test
	public void testIndexCheck() throws Exception {
		int timeoutMs = 10_000;
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});
		createSchema(SCHEMA_NAME, req -> {
			req.addField(
					FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
			req.addField(FieldUtil.createListFieldSchema(MICRONODE_FIELD2_NAME, "micronode")
					.setAllowedSchemas(MICROSCHEMA_NAME));
		});
		waitForSearchIdleEvent();
		syncIndex();

		// trigger the check by publishing the event, and expect the "check finished" but not the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				UnexpectedEvent syncFinished = notExpectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}
	}

	/**
	 * Test adding the first micronode field to a schema
	 * @throws Exception
	 */
	@Test
	public void testAddFirstMicronodeField() throws Exception {
		String stringFieldName = "stringfield";

		SchemaResponse schema = createSchema(SCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(stringFieldName));
		});
		waitForSearchIdleEvent();

		// add some documents
		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");

			request.getFields().put(stringFieldName, FieldUtil.createStringField("bla"));

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			// every 3rd node will be published
			if (i % 3 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}
		waitForSearchIdleEvent();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);

		// create a microschema
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});

		// add a micronode field to the schema
		updateSchema(schema.getUuid(), req -> {
			req.addField(FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
		});
		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);
	}

	/**
	 * Test removing the last micronode field from a schema
	 * @throws Exception
	 */
	@Test
	public void testRemoveFirstMicronodeField() throws Exception {
		String stringFieldName = "stringfield";

		// create a microschema
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});

		// create schema using the microschema
		SchemaResponse schema = createSchema(SCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(stringFieldName));
			req.addField(FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
		});

		// create some nodes
		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");

			// every 2nd node will contain a micronode
			if (i % 2 == 0) {
				request.getFields().put(MICRONODE_FIELD1_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
						Tuple.tuple(MICROSCHEMA_FIELD_NAME, FieldUtil.createStringField("bla"))));
			}

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			// every 3rd node will be published
			if (i % 3 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}

		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);

		// remove the micronode field from the schema
		updateSchema(schema.getUuid(), req -> {
			req.removeField(MICRONODE_FIELD1_NAME);
		});
		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);
	}

	/**
	 * Test adding a second micronode field to a schema
	 * @throws Exception
	 */
	@Test
	public void testAddSecondMicronodeField() throws Exception {
		String stringFieldName = "stringfield";

		// create a microschema
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});

		// create schema using the microschema
		SchemaResponse schema = createSchema(SCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(stringFieldName));
			req.addField(FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
		});

		// create some nodes
		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");

			// every 2nd node will contain a micronode
			if (i % 2 == 0) {
				request.getFields().put(MICRONODE_FIELD1_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
						Tuple.tuple(MICROSCHEMA_FIELD_NAME, FieldUtil.createStringField("bla"))));
			}

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			// every 3rd node will be published
			if (i % 3 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}

		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);

		// add second field using the same microschema
		updateSchema(schema.getUuid(), req -> {
			req.addField(FieldUtil.createListFieldSchema(MICRONODE_FIELD2_NAME, "micronode")
					.setAllowedSchemas(MICROSCHEMA_NAME));
		});
		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);
	}

	/**
	 * Test removing the second micronode field from a schema
	 * @throws Exception
	 */
	@Test
	public void testRemoveSecondMicronodeField() throws Exception {
		String stringFieldName = "stringfield";

		// create a microschema
		createMicroschema(MICROSCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(MICROSCHEMA_FIELD_NAME));
		});

		// create schema using the microschema
		SchemaResponse schema = createSchema(SCHEMA_NAME, req -> {
			req.addField(FieldUtil.createStringFieldSchema(stringFieldName));
			req.addField(FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD1_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
			req.addField(FieldUtil.createListFieldSchema(MICRONODE_FIELD2_NAME, "micronode")
					.setAllowedSchemas(MICROSCHEMA_NAME));
		});

		// create some nodes
		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");

			// every 2nd node will contain a micronode
			if (i % 2 == 0) {
				request.getFields().put(MICRONODE_FIELD1_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
						Tuple.tuple(MICROSCHEMA_FIELD_NAME, FieldUtil.createStringField("bla"))));
			}

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			// every 3rd node will be published
			if (i % 3 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}
		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);

		// remove second field from schema
		updateSchema(schema.getUuid(), req -> {
			req.removeField(MICRONODE_FIELD2_NAME);
		});
		waitForSearchIdleEvent();
		refreshIndices();

		new TestSchemaInfo(SCHEMA_NAME).assertIndices().assertIndexedDraftDocuments(draftNodeUuids)
				.assertIndexedPublishedDocuments(publishedNodeUuids);
	}

	/**
	 * Create a schema with given name
	 * @param name schema name
	 * @param requestConsumer consumer for setup of the schema create request
	 * @return created schema
	 */
	protected SchemaResponse createSchema(String name, Consumer<SchemaCreateRequest> requestConsumer) {
		SchemaCreateRequest createSchema = new SchemaCreateRequest();
		createSchema.setName(name);
		requestConsumer.accept(createSchema);
		SchemaResponse schema = call(() -> client().createSchema(createSchema));

		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		return schema;
	}

	/**
	 * Update the schema with uuid
	 * @param uuid schema uuid
	 * @param updater updater consumer
	 */
	protected void updateSchema(String uuid, Consumer<Schema> updater) {
		waitForJob(() -> {
			SchemaResponse schema = call(() -> client().findSchemaByUuid(uuid));
			updater.accept(schema);
			SchemaUpdateRequest updateSchema = JsonUtil.readValue(schema.toJson(), SchemaUpdateRequest.class);
			call(() -> client().updateSchema(uuid, updateSchema,
					new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)));
		});
	}

	/**
	 * Create a microschema with given name
	 * @param name microschema name
	 * @param requestConsumer consumer for setup of the microschema create request
	 * @return created microschema
	 */
	protected MicroschemaResponse createMicroschema(String name, Consumer<MicroschemaCreateRequest> requestConsumer) {
		MicroschemaCreateRequest createMicroschema = new MicroschemaCreateRequest();
		createMicroschema.setName(name);
		requestConsumer.accept(createMicroschema);
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicroschema));

		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		return microschema;
	}

	/**
	 * Update the microschema with uuid
	 * @param uuid microschema uuid
	 * @param updater updater consumer
	 */
	protected void updateMicroschema(String uuid, Consumer<Microschema> updater) {
		waitForJob(() -> {
			MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(uuid));
			updater.accept(microschema);
			MicroschemaUpdateRequest updateMicroschema = JsonUtil.readValue(microschema.toJson(),
					MicroschemaUpdateRequest.class);
			call(() -> client().updateMicroschema(uuid, updateMicroschema,
					new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)));
		});
	}

	/**
	 * Get the document IDs of all documents in the given index
	 * @param indexName index name
	 * @return set of document IDs
	 * @throws HttpErrorException
	 */
	protected Set<String> getIndexDocumentIds(String indexName) throws HttpErrorException {
		ElasticsearchClient<JsonObject> client = searchProvider().getClient();
		JsonObject query = new JsonObject().put("query",
				new JsonObject().put("query_string", new JsonObject().put("query", "*")));
		JsonObject searchResult = client.search(query, indexName).sync();
		List<String> ids = JsonPath.read(searchResult.encode(), "$.hits.hits[*]._id");
		return new HashSet<>(ids);
	}

	public class TestSchemaInfo {
		public String schemaVersionUuid;

		public String expectedDraftIndex;

		public String expectedPublishedIndex;

		public TestSchemaInfo(String schemaName) throws Exception {
			// get the uuid of the schema version, which is assigned to the branch
			BranchInfoSchemaList schemaList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, branchUuid));
			BranchSchemaInfo schemaInfo = schemaList.getSchemas().stream()
					.filter(branchSchemaInfo -> StringUtils.equals(schemaName, branchSchemaInfo.getName()))
					.findFirst()
					.orElseThrow(() -> new Exception("Schema was not assigned to branch"));
			schemaVersionUuid = schemaInfo.getVersionUuid();

			// get the used microschemas
			SchemaResponse schema = call(() -> client().findSchemaByUuid(schemaInfo.getUuid(), new VersioningParametersImpl().setBranch(schemaInfo.getVersion())));
			Set<String> microschemaNames = schema.getFields().stream().flatMap(field -> {
				if (field instanceof MicronodeFieldSchema) {
					return Stream.of(((MicronodeFieldSchema) field).getAllowedMicroSchemas());
				} else if (field instanceof ListFieldSchema) {
					if (Objects.equals(((ListFieldSchema) field).getListType(), "micronode")) {
						return Stream.of(((ListFieldSchema) field).getAllowedSchemas());
					} else {
						return Stream.empty();
					}
				} else {
					return Stream.empty();
				}
			}).collect(Collectors.toSet());

			// get the uuid of the microschema version, which is assigned to the branch
			BranchInfoMicroschemaList microschemaList = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, branchUuid));

			Set<String> microschemaUuids = new TreeSet<>();
			for (String microschema : microschemaNames) {
				microschemaUuids.add(microschemaList.getMicroschemas().stream()
						.filter(branchMicroschemaInfo -> StringUtils.equals(microschema,
								branchMicroschemaInfo.getName()))
						.map(BranchMicroschemaInfo::getVersionUuid).findFirst().orElseThrow(
								() -> new Exception("Microschema " + microschema + " was not assigned to branch")));
			}

			if (microschemaUuids.isEmpty()) {
				expectedDraftIndex = String.format("mesh-node-%s-%s-%s-draft", project.getUuid(), branchUuid, schemaVersionUuid);
				expectedPublishedIndex = String.format("mesh-node-%s-%s-%s-published", project.getUuid(), branchUuid, schemaVersionUuid);
			} else {
				String expectedHash = DigestUtils.md5Hex(microschemaUuids.stream().collect(Collectors.joining("|")));
				expectedDraftIndex = String.format("mesh-node-%s-%s-%s-draft-%s", project.getUuid(), branchUuid, schemaVersionUuid, expectedHash);
				expectedPublishedIndex = String.format("mesh-node-%s-%s-%s-published-%s", project.getUuid(), branchUuid, schemaVersionUuid, expectedHash);
			}
		}

		/**
		 * Assert that only the expected indices exist for the project/branch/schema version
		 * @return fluent API
		 * @throws Exception
		 */
		public TestSchemaInfo assertIndices() throws Exception {
			ElasticsearchClient<JsonObject> client = searchProvider().getClient();
			String indexNamePattern = String.format("mesh-node-%s-%s-%s*", project.getUuid(), branchUuid, schemaVersionUuid);
			JsonObject indexResponse = client.readIndex(indexNamePattern).sync();
			assertThat(indexResponse.fieldNames()).as("Index names").containsOnly(expectedDraftIndex, expectedPublishedIndex);
			return this;
		}

		/**
		 * Assert that the draft index contains only the documents with given uuid (and language "en")
		 * @param indexName index name
		 * @param uuids expected uuids
		 * @return fluent API
		 * @throws HttpErrorException
		 */
		public TestSchemaInfo assertIndexedDraftDocuments(Set<String> uuids) throws HttpErrorException {
			assertThat(getIndexDocumentIds(expectedDraftIndex)).as("Documents in index " + expectedDraftIndex).containsOnlyElementsOf(
					uuids.stream().map(uuid -> NodeGraphFieldContainer.composeDocumentId(uuid, "en"))
							.collect(Collectors.toSet()));
			return this;
		}

		/**
		 * Assert that the published index contains only the documents with given uuid (and language "en")
		 * @param indexName index name
		 * @param uuids expected uuids
		 * @return fluent API
		 * @throws HttpErrorException
		 */
		public TestSchemaInfo assertIndexedPublishedDocuments(Set<String> uuids) throws HttpErrorException {
			assertThat(getIndexDocumentIds(expectedPublishedIndex)).as("Documents in index " + expectedPublishedIndex).containsOnlyElementsOf(
					uuids.stream().map(uuid -> NodeGraphFieldContainer.composeDocumentId(uuid, "en"))
							.collect(Collectors.toSet()));
			return this;
		}

	}
}

package com.gentics.mesh.search.index;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.mesh.FieldUtil;
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
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.helper.ExpectedEvent;
import com.gentics.mesh.test.context.helper.UnexpectedEvent;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for elasticsearch indices for schemas using microschemas
 */
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class MicronodeIndexSyncTest extends AbstractMeshTest {
	protected final static String PROJECT_NAME = "testproject";

	protected final static String SCHEMA_NAME = "schema";

	protected final static String MICROSCHEMA_NAME = "micro";

	private ProjectResponse project;

	private String branchUuid;

	private MicroschemaResponse microschema;

	private SchemaResponse schema;

	private String schemaVersionUuid;

	private String microschemaVersionUuid;

	private String expectedDraftIndex;

	private String expectedPublishedIndex;

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

		microschema = createTestMicroschema();
		schema = createTestSchema();

		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));
		waitForSearchIdleEvent();

		syncIndex();
		updateIndexNames();
	}

	/**
	 * Test that the indices were created with the expected names (containing hash over the microschema version uuids)
	 * @throws Exception
	 */
	@Test
	public void testIndexCreation() throws Exception {
		ElasticsearchClient<JsonObject> client = searchProvider().getClient();
		String indexNamePattern = String.format("mesh-node-%s-%s-%s-*", project.getUuid(), branchUuid, schemaVersionUuid);
		JsonObject indexResponse = client.readIndex(indexNamePattern).sync();

		assertThat(indexResponse.fieldNames()).as("Index names").containsOnly(expectedDraftIndex, expectedPublishedIndex);
	}

	/**
	 * Test that updating the microschema creates new indices and migrates all data
	 * @throws Exception
	 */
	@Test
	public void testUpdateMicroschema() throws Exception {
		ElasticsearchClient<JsonObject> client = searchProvider().getClient();

		Set<String> draftNodeUuids = new HashSet<>();
		Set<String> publishedNodeUuids = new HashSet<>();
		for (int i = 0; i < 5; i++) {
			NodeCreateRequest request = new NodeCreateRequest()
					.setSchemaName(SCHEMA_NAME)
					.setParentNodeUuid(project.getRootNode().getUuid())
					.setLanguage("en");
			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, request));
			draftNodeUuids.add(node.getUuid());

			if (i % 2 == 0) {
				call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
				publishedNodeUuids.add(node.getUuid());
			}
		}

		waitForSearchIdleEvent();

		for (String uuid : draftNodeUuids) {
			client.getDocument(expectedDraftIndex, uuid);
		}
		for (String uuid : publishedNodeUuids) {
			client.getDocument(expectedPublishedIndex, uuid);
		}

		String oldDraftIndex = expectedDraftIndex;
		String oldPublishedString = expectedPublishedIndex;
		updateTestMicroschema(microschema.getUuid(), update -> {
			update.addField(new StringFieldSchemaImpl().setName("two"));
		});

		updateIndexNames();
		waitForSearchIdleEvent();

		assertThat(expectedDraftIndex).as("draft index name").isNotEqualTo(oldDraftIndex);
		assertThat(expectedPublishedIndex).as("published index name").isNotEqualTo(oldPublishedString);

		for (String uuid : draftNodeUuids) {
			client.getDocument(expectedDraftIndex, uuid);
		}
		for (String uuid : publishedNodeUuids) {
			client.getDocument(expectedPublishedIndex, uuid);
		}
	}

	/**
	 * Test that the index check will accept the indices
	 * @throws Exception
	 */
	@Test
	public void testIndexCheck() throws Exception {
		int timeoutMs = 10_000;

		// trigger the check by publishing the event, and expect the "check finished" but not the "sync finished" events to be thrown
		try (ExpectedEvent finished = expectEvent(MeshEvent.INDEX_CHECK_FINISHED, timeoutMs);
				UnexpectedEvent syncFinished = notExpectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeoutMs)) {
			vertx().eventBus().publish(MeshEvent.INDEX_CHECK_REQUEST.address, null);
		}
	}

	/**
	 * Set the expected index names for the schema indices, based on the currently assigned schema version and microschema version
	 * @throws Exception
	 */
	protected void updateIndexNames() throws Exception {
		// get the uuid of the schema version, which is assigned to the branch
		BranchInfoSchemaList schemaList = call(() -> client().getBranchSchemaVersions(PROJECT_NAME, branchUuid));
		schemaVersionUuid = schemaList.getSchemas().stream()
				.filter(branchSchemaInfo -> StringUtils.equals(SCHEMA_NAME, branchSchemaInfo.getName()))
				.map(BranchSchemaInfo::getVersionUuid).findFirst()
				.orElseThrow(() -> new Exception("Schema was not assigned to branch"));

		// get the uuid of the microschema version, which is assigned to the branch
		BranchInfoMicroschemaList microschemaList = call(() -> client().getBranchMicroschemaVersions(PROJECT_NAME, branchUuid));
		microschemaVersionUuid = microschemaList.getMicroschemas().stream()
				.filter(branchMicroschemaInfo -> StringUtils.equals(MICROSCHEMA_NAME, branchMicroschemaInfo.getName()))
				.map(BranchMicroschemaInfo::getVersionUuid).findFirst()
				.orElseThrow(() -> new Exception("Microschema was not assigned to branch"));

		String expectedHash = DigestUtils.md5Hex(microschemaVersionUuid);
		expectedDraftIndex = String.format("mesh-node-%s-%s-%s-draft-%s", project.getUuid(), branchUuid, schemaVersionUuid, expectedHash);
		expectedPublishedIndex = String.format("mesh-node-%s-%s-%s-published-%s", project.getUuid(), branchUuid, schemaVersionUuid, expectedHash);
	}

	/**
	 * Create the test microschema
	 * @return microschema response
	 */
	protected MicroschemaResponse createTestMicroschema() {
		MicroschemaCreateRequest createMicroschema = new MicroschemaCreateRequest();
		createMicroschema.setName(MICROSCHEMA_NAME);
		createMicroschema.addField(FieldUtil.createStringFieldSchema("one"));
		return call(() -> client().createMicroschema(createMicroschema));
	}

	/**
	 * Update the microschema with uuid
	 * @param uuid microschema uuid
	 * @param updater updater consumer
	 */
	protected void updateTestMicroschema(String uuid, Consumer<Microschema> updater) {
		waitForJob(() -> {
			MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(uuid));
			updater.accept(microschema);
			MicroschemaUpdateRequest updateMicroschema = JsonUtil.readValue(microschema.toJson(), MicroschemaUpdateRequest.class);
			call(() -> client().updateMicroschema(uuid, updateMicroschema, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)));
		});
	}

	/**
	 * Create the test schema
	 * @return schema response
	 */
	protected SchemaResponse createTestSchema() {
		SchemaCreateRequest createSchema = new SchemaCreateRequest();
		createSchema.setName(SCHEMA_NAME);
		createSchema.addField(FieldUtil.createMicronodeFieldSchema("microfield").setAllowedMicroSchemas(MICROSCHEMA_NAME));
		return call(() -> client().createSchema(createSchema));
	}
}

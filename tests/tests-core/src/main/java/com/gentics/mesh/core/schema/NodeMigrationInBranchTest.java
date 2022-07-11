package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.Tuple;

/**
 * Test cases for doing node or micronode migrations in branches
 */
@MeshTestSetting(testSize = PROJECT, startServer = true)
public class NodeMigrationInBranchTest extends AbstractMeshTest {
	public final static String DEFAULT_BRANCH_NAME = PROJECT_NAME;
	public final static String BRANCH_NAME = "newbranch";
	public final static String MICROSCHEMA_NAME = "microschema";
	public final static String SCHEMA_NAME = "migration_schema";
	public final static String STRING_FIELD_NAME = "stringfield";
	public final static String NEW_STRING_FIELD_NAME = "new_stringfield";
	public final static String MICRONODE_FIELD_NAME = "micronodefield";

	private MicroschemaResponse microschema;
	private SchemaResponse schema;
	private NodeResponse newInDefault;
	private NodeResponse publishedInDefault;
	private NodeResponse modifiedInDefault;
	private NodeResponse newInBranch;
	private NodeResponse publishedInBranch;
	private NodeResponse modifiedInBranch;

	/**
	 * Prepare test data (microschema, schema, branch and some nodes)
	 */
	@Before
	public void setup() {
		// create a microschema
		MicroschemaCreateRequest microschemaCreateRequest = new MicroschemaCreateRequest().setName(MICROSCHEMA_NAME);
		microschemaCreateRequest.addField(FieldUtil.createStringFieldSchema(STRING_FIELD_NAME));
		microschema = call(() -> client().createMicroschema(microschemaCreateRequest));

		// assign microschema to project
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// create a schema
		SchemaCreateRequest createSchemaRequest = new SchemaCreateRequest().setName(SCHEMA_NAME);
		createSchemaRequest.addField(FieldUtil.createStringFieldSchema(STRING_FIELD_NAME));
		createSchemaRequest.addField(FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD_NAME));
		schema = call(() -> client().createSchema(createSchemaRequest));

		// assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// create new branch
		call(() -> client().createBranch(PROJECT_NAME,
				new BranchCreateRequest().setName(BRANCH_NAME).setLatest(false)));

		// create some nodes
		ProjectResponse project = call(() -> client().findProjectByName(PROJECT_NAME));
		newInDefault = createNode(project.getRootNode().getUuid(), DEFAULT_BRANCH_NAME, false, false);
		publishedInDefault = createNode(project.getRootNode().getUuid(), DEFAULT_BRANCH_NAME, true, false);
		modifiedInDefault = createNode(project.getRootNode().getUuid(), DEFAULT_BRANCH_NAME, true, true);
		newInBranch = createNode(project.getRootNode().getUuid(), BRANCH_NAME, false, false);
		publishedInBranch = createNode(project.getRootNode().getUuid(), BRANCH_NAME, true, false);
		modifiedInBranch = createNode(project.getRootNode().getUuid(), BRANCH_NAME, true, true);
	}

	/**
	 * Test whether nodes in both branches will be migrated, when the schema is modified
	 */
	@Test
	public void testNodeMigration() {
		waitForJobs(() -> {
			SchemaUpdateRequest updateSchema = new SchemaUpdateRequest();
			updateSchema.setName(SCHEMA_NAME);
			updateSchema.addField(FieldUtil.createStringFieldSchema(NEW_STRING_FIELD_NAME));
			call(() -> client().updateSchema(schema.getUuid(), updateSchema));
		}, JobStatus.COMPLETED, 2);

		// assert versions of migrated nodes
		assertVersions(newInDefault.getUuid(), "en", "D(0.2)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(publishedInDefault.getUuid(), "en", "PD(2.0)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(modifiedInDefault.getUuid(), "en", "D(2.1)=>P(2.0)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(newInBranch.getUuid(), "en", "D(0.2)=>I(0.1)", BRANCH_NAME);
		assertVersions(publishedInBranch.getUuid(), "en", "PD(2.0)=>I(0.1)", BRANCH_NAME);
		assertVersions(modifiedInBranch.getUuid(), "en", "D(2.1)=>P(2.0)=>I(0.1)", BRANCH_NAME);
	}

	/**
	 * Test whether nodes in both branches will be migrated, when the microschema is modified
	 */
	@Test
	public void testMicronodeMigration() {
		waitForJobs(() -> {
			MicroschemaUpdateRequest updateRequest = new MicroschemaUpdateRequest();
			updateRequest.setName(MICROSCHEMA_NAME);
			updateRequest.addField(FieldUtil.createStringFieldSchema(NEW_STRING_FIELD_NAME));
			call(() -> client().updateMicroschema(microschema.getUuid(), updateRequest));
		}, JobStatus.COMPLETED, 2);

		// assert versions of migrated nodes
		assertVersions(newInDefault.getUuid(), "en", "D(0.2)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(publishedInDefault.getUuid(), "en", "PD(2.0)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(modifiedInDefault.getUuid(), "en", "D(2.1)=>P(2.0)=>I(0.1)", DEFAULT_BRANCH_NAME);
		assertVersions(newInBranch.getUuid(), "en", "D(0.2)=>I(0.1)", BRANCH_NAME);
		assertVersions(publishedInBranch.getUuid(), "en", "PD(2.0)=>I(0.1)", BRANCH_NAME);
		assertVersions(modifiedInBranch.getUuid(), "en", "D(2.1)=>P(2.0)=>I(0.1)", BRANCH_NAME);
	}

	/**
	 * Create a node in a branch
	 * @param parentNodeUuid uuid of the parent node
	 * @param branchName name of the branch
	 * @param publish true to publish the node
	 * @param modify true to modify the node
	 * @return node response
	 */
	protected NodeResponse createNode(String parentNodeUuid, String branchName, boolean publish, boolean modify) {
		NodeCreateRequest createNodeRequest = new NodeCreateRequest().setLanguage("en")
				.setSchemaName(SCHEMA_NAME).setParentNodeUuid(parentNodeUuid);
		createNodeRequest.getFields().put(STRING_FIELD_NAME, FieldUtil.createStringField("field value"));
		createNodeRequest.getFields().put(MICRONODE_FIELD_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
				Tuple.tuple(STRING_FIELD_NAME, FieldUtil.createStringField("micro value"))));
		NodeResponse node = call(() -> client().createNode(PROJECT_NAME, createNodeRequest, new VersioningParametersImpl().setBranch(branchName)));

		if (publish) {
			call(() -> client().publishNode(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchName)));
		}

		if (modify) {
			NodeUpdateRequest updateNodeRequest = new NodeUpdateRequest().setLanguage("en").setVersion("draft");
			updateNodeRequest.getFields().put(STRING_FIELD_NAME, FieldUtil.createStringField("modified field value"));
			updateNodeRequest.getFields().put(MICRONODE_FIELD_NAME, FieldUtil.createMicronodeField(MICROSCHEMA_NAME,
					Tuple.tuple(STRING_FIELD_NAME, FieldUtil.createStringField("modified micro value"))));

			call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), updateNodeRequest, new VersioningParametersImpl().setBranch(branchName)));
		}

		return node;
	}
}

package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.cache.GraphQLSchemaCache;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLErrorModel;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.jayway.jsonpath.JsonPath;

/**
 * Test cases for cache invalidation of {@link GraphQLSchemaCache}.
 */
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLSchemaCacheTest extends AbstractMeshTest {
	/**
	 * Get error messages from response
	 * @param response response
	 * @return error messages
	 */
	protected static List<String> errorMessages(GraphQLResponse response) {
		List<GraphQLErrorModel> errors = response.getErrors();

		if (CollectionUtils.isEmpty(errors)) {
			return Collections.emptyList();
		} else {
			return errors.stream().map(GraphQLErrorModel::getMessage).collect(Collectors.toList());
		}
	}

	/**
	 * Consumer that asserts that the response contains no errors
	 */
	protected final static Consumer<GraphQLResponse> noErrors = response -> {
		assertThat(errorMessages(response)).as("Response errors").isNullOrEmpty();
	};

	/**
	 * Consumer that asserts that the response contains a ValidationError
	 */
	protected final static Consumer<GraphQLResponse> validationError = response -> {
		assertThat(response.getErrors()).usingElementComparatorOnFields("type").contains(new GraphQLErrorModel().setType("ValidationError"));
	};

	/**
	 * Test adding a field to a schema
	 */
	@Test
	public void testSchemaChange() {
		String jsonPathToUuid = "$.schema.uuid";

		// get a node with its fields via graphQl
		String responseData = executeQuery(getNodesQuery("folder", "slug", "name"), noErrors);
		String schemaUuid = JsonPath.read(responseData, jsonPathToUuid);

		SchemaResponse schema = call(() -> client().findSchemaByUuid(schemaUuid));

		// add field to schema
		SchemaUpdateRequest update = new SchemaUpdateRequest();
		update.setName("folder");
		update.setFields(schema.getFields());
		update.addField(FieldUtil.createStringFieldSchema("newfield"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, update));
		}, JobStatus.COMPLETED, 1);

		// get a node again
		responseData = executeQuery(getNodesQuery("folder", "slug", "name", "newfield"), noErrors);
	}

	/**
	 * Test assigning a schema to a project
	 */
	@Test
	public void testSchemaAssign() {
		// create a new schema
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("newschema");
		create.addField(FieldUtil.createStringFieldSchema("newfield"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// get a node of schema "folder" with its fields via graphQl
		executeQuery(getNodesQuery("folder", "slug", "name"), noErrors);

		// getting the nodes of the new schema should produce a ValidationError
		executeQuery(getNodesQuery("newschema", "newfield"), null, validationError);

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// try to get nodes of new schema
		executeQuery(getNodesQuery("newschema", "newfield"), noErrors);
	}

	/**
	 * Test unassigning a schema from a project
	 */
	@Test
	public void testSchemaUnassign() {
		// create a new schema
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("newschema");
		create.addField(FieldUtil.createStringFieldSchema("newfield"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// try to get nodes of new schema
		executeQuery(getNodesQuery("newschema", "newfield"), noErrors);

		// unassign schema from project
		call(() -> client().unassignSchemaFromProject(PROJECT_NAME, newSchema.getUuid()));

		// getting the nodes of the new schema should produce a ValidationError
		executeQuery(getNodesQuery("newschema", "newfield"), null, validationError);
	}

	/**
	 * Test deleting a schema
	 */
	@Test
	public void testSchemaDelete() {
		// create a new schema
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("newschema");
		create.addField(FieldUtil.createStringFieldSchema("newfield"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// try to get nodes of new schema
		executeQuery(getNodesQuery("newschema", "newfield"), noErrors);

		// delete the schema
		call(() -> client().deleteSchema(newSchema.getUuid()));

		// try to get nodes of new schema
		executeQuery(getNodesQuery("newschema", "newfield"), validationError);
	}

	/**
	 * Test adding a field to a microschema
	 */
	@Test
	public void testMicroschemaChange() {
		// create a new schema with micronode field
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("withmicronode");
		create.addField(FieldUtil.createMicronodeFieldSchema("micronode"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// create a new microschema
		MicroschemaCreateRequest createMicro = new MicroschemaCreateRequest();
		createMicro.setName("microschema");
		createMicro.addField(FieldUtil.createStringFieldSchema("string"));
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicro));

		// assign microschema to project
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// get nodes with micronodes
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), noErrors);

		// add field to microschema
		waitForJobs(() -> {
			call(() -> {
				MicroschemaUpdateRequest update = new MicroschemaUpdateRequest();
				update.setName(microschema.getName());
				update.setFields(microschema.getFields());
				update.addField(FieldUtil.createStringFieldSchema("newfield"));
				return client().updateMicroschema(microschema.getUuid(), update);
			});
		}, JobStatus.COMPLETED, 1);

		// get nodes with micronodes containing new fields
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string", "newfield"), noErrors);
	}

	/**
	 * Test assigning a microschema to a project
	 */
	@Test
	public void testMicroschemaAssign() {
		// create a new schema with micronode field
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("withmicronode");
		create.addField(FieldUtil.createMicronodeFieldSchema("micronode"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// create a new microschema
		MicroschemaCreateRequest createMicro = new MicroschemaCreateRequest();
		createMicro.setName("microschema");
		createMicro.addField(FieldUtil.createStringFieldSchema("string"));
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicro));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), validationError);

		// assign microschema
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), noErrors);
	}

	/**
	 * Test unassigning a microschema from a project
	 */
	@Test
	public void testMicroschemaUnassign() {
		// create a new schema with micronode field
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("withmicronode");
		create.addField(FieldUtil.createMicronodeFieldSchema("micronode"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// create a new microschema
		MicroschemaCreateRequest createMicro = new MicroschemaCreateRequest();
		createMicro.setName("microschema");
		createMicro.addField(FieldUtil.createStringFieldSchema("string"));
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicro));

		// assign microschema
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), noErrors);

		// unassign microschema
		call(() -> client().unassignMicroschemaFromProject(PROJECT_NAME, microschema.getUuid()));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), validationError);
	}

	/**
	 * Test deleting a microschema
	 */
	@Test
	public void testMicroschemaDelete() {
		// create a new schema with micronode field
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("withmicronode");
		create.addField(FieldUtil.createMicronodeFieldSchema("micronode"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// create a new microschema
		MicroschemaCreateRequest createMicro = new MicroschemaCreateRequest();
		createMicro.setName("microschema");
		createMicro.addField(FieldUtil.createStringFieldSchema("string"));
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicro));

		// assign microschema
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), noErrors);

		// delete microschema
		call(() -> client().deleteMicroschema(microschema.getUuid()));

		// try to get nodes with microschema
		executeQuery(getMicronodesQuery("withmicronode", "micronode", "microschema", "string"), validationError);
	}

	/**
	 * Test that the cache separates instances by project
	 */
	@Test
	public void testProjectSeparation() {
		call(() -> {
			ProjectCreateRequest create = new ProjectCreateRequest();
			create.setName("newproject");
			create.setSchemaRef("folder");

			return client().createProject(create);
		});

		// test query for the test project
		executeQuery(PROJECT_NAME, 2, getNodesQuery("content", "slug"), null, noErrors);

		// test query for the new project
		executeQuery("newproject", 2, getNodesQuery("content", "slug"), null, validationError);
	}

	/**
	 * Test that the cache separates instances by branch
	 */
	@Test
	@Ignore("This test currently fails, because the GraphQLSchema will always use the latest version of the schema, not the version, which is assigned to the branch")
	public void testBranchSeparation() {
		// create a new schema
		SchemaCreateRequest create = new SchemaCreateRequest();
		create.setName("newschema");
		create.addField(FieldUtil.createStringFieldSchema("newfield"));
		SchemaResponse newSchema = call(() -> client().createSchema(create));

		// assign new schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, newSchema.getUuid()));

		// create branch in project
		waitForJobs(() -> {
			BranchCreateRequest createBranch = new BranchCreateRequest();
			createBranch.setName("newbranch");
			createBranch.setLatest(false);
			call(() -> client().createBranch(PROJECT_NAME, createBranch));
		}, JobStatus.COMPLETED, 1);

		AtomicReference<String> initialBranchUuid = new AtomicReference<>();
		AtomicReference<String> newBranchUuid = new AtomicReference<>();

		for (BranchResponse branch : call(() -> client().findBranches(PROJECT_NAME)).getData()) {
			if (StringUtils.equals(branch.getName(), PROJECT_NAME)) {
				initialBranchUuid.set(branch.getUuid());
			} else if (StringUtils.equals(branch.getName(), "newbranch")) {
				newBranchUuid.set(branch.getUuid());
			}
		}

		// update schema and assign to initial branch
		call(() -> {
			SchemaUpdateRequest update = new SchemaUpdateRequest();
			update.setName("newschema");
			update.setFields(newSchema.getFields());
			update.addField(FieldUtil.createStringFieldSchema("field_in_initialbranch"));
			return client().updateSchema(newSchema.getUuid(), update,
					new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));
		});

		String initialBranchVersion = call(() -> client().findSchemaByUuid(newSchema.getUuid())).getVersion();

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid.get(),
					new SchemaReferenceImpl().setName("newschema").setVersion(initialBranchVersion)));
		}, JobStatus.COMPLETED, 1);

		// update schema again and assign to new branch
		call(() -> {
			SchemaUpdateRequest update = new SchemaUpdateRequest();
			update.setName("newschema");
			update.setFields(newSchema.getFields());
			update.removeField("field_in_initialbranch");
			update.addField(FieldUtil.createStringFieldSchema("field_in_newbranch"));
			return client().updateSchema(newSchema.getUuid(), update,
					new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false));
		});

		String newBranchVersion = call(() -> client().findSchemaByUuid(newSchema.getUuid())).getVersion();

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, newBranchUuid.get(),
					new SchemaReferenceImpl().setName("newschema").setVersion(newBranchVersion)));
		}, JobStatus.COMPLETED, 1);

		// get nodes for initial branch
		executeQuery(getNodesQuery("newschema", "newfield", "field_in_initialbranch"), PROJECT_NAME, noErrors);

		// get nodes for new branch
		executeQuery(getNodesQuery("newschema", "newfield", "field_in_newbranch"), "newbranch", noErrors);
	}

	/**
	 * Test that the cache separates instances by API Version
	 */
	@Test
	public void testAPIVersionSeparation() {
		// executing the query with API v2 succeeds
		executeQuery(PROJECT_NAME, 2, getNodesQuery("folder", "slug", "name"), null, noErrors);

		// executing the same query with API v1 fails
		executeQuery(PROJECT_NAME, 1, getNodesQuery("folder", "slug", "name"), null, validationError);
	}

	/**
	 * Get the graphQl Query to fetch nodes with fields for a schema
	 * @param schemaName schema name
	 * @param fields fields to fetch for the schema
	 * @return query
	 */
	protected String getNodesQuery(String schemaName, String...fields) {
		return String.format("{ schema(name: \"%s\") { uuid nodes { elements { ... on %s { fields { %s } } } } } }", schemaName, schemaName, StringUtils.join(fields, " "));
	}

	/**
	 * Get the graphQL query to fetch nodes with a micronode field
	 * @param schemaName schema name
	 * @param micronodeFieldName micronode field name
	 * @param microschemaName microschema name
	 * @param fields microschema field names
	 * @return query
	 */
	protected String getMicronodesQuery(String schemaName, String micronodeFieldName, String microschemaName,
			String... fields) {
		return String.format(
				"{ schema(name: \"%s\") { nodes { elements { ... on %s { fields { %s { ... on %s { fields { %s } } } } } } } } }",
				schemaName, schemaName, micronodeFieldName, microschemaName, StringUtils.join(fields, " "));
	}

	/**
	 * Execute the query with API v2 for the default branch, pass the response to the asserter and return the data as json string
	 * @param query query to perform
	 * @param asserter consumer to check the response
	 * @return data as json string
	 */
	protected String executeQuery(String query, Consumer<GraphQLResponse> asserter) {
		return executeQuery(query, null, asserter);
	}

	/**
	 * Execute the query with API v2 for the given branch (default branch if no branchName is given), pass the response to the asserter and return the data as json string
	 * @param query query to perform
	 * @param branchName branch name (null for default branch)
	 * @param asserter consumer to check the response
	 * @return data as json string
	 */
	protected String executeQuery(String query, String branchName, Consumer<GraphQLResponse> asserter) {
		return executeQuery(PROJECT_NAME, 2, query, branchName, asserter);
	}

	/**
	 * Execute the query with given API version for the given branch (default branch if no branchName is given), pass the response to the asserter and return the data as json string
	 * @param projectName project Name
	 * @param apiVersion api Version
	 * @param query query to perform
	 * @param branchName branch name (null for default branch)
	 * @param asserter consumer to check the response
	 * @return data as json string
	 */
	protected String executeQuery(String projectName, int apiVersion, String query, String branchName, Consumer<GraphQLResponse> asserter) {
		GraphQLResponse response = null;

		if (StringUtils.isEmpty(branchName)) {
			response = call(() -> client(String.format("v%d", apiVersion)).graphqlQuery(projectName, query));
		} else {
			response = call(() -> client(String.format("v%d", apiVersion)).graphqlQuery(projectName, query, new VersioningParametersImpl().setBranch(branchName)));
		}
		if (asserter != null) {
			asserter.accept(response);
		}
		if (response.getData() != null) {
			return response.getData().toString();
		} else {
			return null;
		}
	}

}

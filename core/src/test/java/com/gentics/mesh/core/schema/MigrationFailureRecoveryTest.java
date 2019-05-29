package com.gentics.mesh.core.schema;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

/**
 * Tests for https://github.com/gentics/mesh/issues/532
 * Tests if a schema or microschema can be updated after a failed migration.
 */
@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = false)
public class MigrationFailureRecoveryTest extends AbstractMeshTest {
	private static final String SCHEMA_NAME = "testSchema";
	private static final String MICROSCHEMA_NAME = "testMicroschema";

	private Single<ProjectResponse> project$ = Single.defer(() -> client().findProjectByName(PROJECT_NAME).toSingle()).cache();
	private Single<BranchResponse> branch$ = Single.defer(() -> client().findBranches(PROJECT_NAME).toSingle()).map(it -> it.getData().get(0)).cache();


	@Test
	public void testUpdateSchemaAfterFailingMigration() {
		fixPermissions();
		SchemaResponse schema = createSchema();
		createNodes("nodeOne", "nodeOne", "nodeTwo");

		waitForLatestJob(() -> invokeFailingMigration(schema), JobStatus.FAILED);
		waitForLatestJob(() -> updateSchema(schema), JobStatus.COMPLETED);
		waitForLatestJob(this::migrateSchemas, JobStatus.FAILED);
	}

	@Test
	public void testUpdateMicroSchemaAfterFailingMigration() {
		fixPermissions();
		MicroschemaResponse microschema = createMicroschema();
		createSchema(microschema);
		createNodes(true, "nodeOne", "nodeOne", "nodeTwo");

		System.out.println(client().findJobs().toSingle().map(RestModel::toJson).blockingGet());
		waitForLatestJob(() -> invokeFailingMigration(microschema), JobStatus.FAILED);
		System.out.println(client().findJobs().toSingle().map(RestModel::toJson).blockingGet());
		waitForLatestJob(() -> updateSchema(microschema), JobStatus.COMPLETED);
		System.out.println(client().findJobs().toSingle().map(RestModel::toJson).blockingGet());
		waitForLatestJob(this::migrateMicroschemas, JobStatus.FAILED);
		System.out.println(client().findJobs().toSingle().map(RestModel::toJson).blockingGet());
	}

	private void fixPermissions() {
		GroupResponse adminGroup = client().findGroups()
			.toSingle()
			.to(this::listObservable)
			.filter(group -> group.getName().equals("admin"))
			.firstOrError()
			.blockingGet();

		UserResponse user = client().me().toSingle().blockingGet();
		client().addUserToGroup(adminGroup.getUuid(), user.getUuid()).toSingle().blockingGet();
	}

	private <T> Observable<T> listObservable(Single<? extends ListResponse<T>> upstream) {
		return upstream.flatMapObservable(response -> Observable.fromIterable(response.getData()));
	}

	private void createNodes(String... names) {
		createNodes(false, names);
	}

	private void createNodes(boolean withMicronodes, String... names) {
		project$.flatMapObservable(project ->
			Observable.fromArray(names)
				.flatMapSingle(name -> {
					NodeCreateRequest request = new NodeCreateRequest();
					request.setParentNode(project.getRootNode());
					request.setLanguage("en");
					request.setSchemaName(SCHEMA_NAME);
					request.getFields().put("name", new StringFieldImpl().setString(name));
					if (withMicronodes) {
						MicronodeResponse micronodeResponse = new MicronodeResponse();
						micronodeResponse.setMicroschema(new MicroschemaReferenceImpl().setName(MICROSCHEMA_NAME));
						request.getFields().put("micronode", micronodeResponse);
					}
					return client().createNode(PROJECT_NAME, request).toSingle();
				})
		).blockingSubscribe();
	}

	private SchemaResponse createSchema() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Collections.singletonList(
			new StringFieldSchemaImpl().setName("name")
		));
		SchemaResponse schema = client().createSchema(request).toSingle().blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()).toCompletable().blockingAwait();
		return schema;
	}

	private SchemaResponse createSchema(MicroschemaResponse microschema) {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Arrays.asList(
			new StringFieldSchemaImpl().setName("name"),
			new MicronodeFieldSchemaImpl().setAllowedMicroSchemas(microschema.getName()).setName("micronode")
		));
		SchemaResponse schema = client().createSchema(request).toSingle().blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()).toCompletable().blockingAwait();
		return schema;
	}

	private MicroschemaResponse createMicroschema() {
		MicroschemaResponse microschema = createMicroschema(MICROSCHEMA_NAME);

		client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()).toCompletable().blockingAwait();
		return microschema;
	}

	private void invokeFailingMigration(SchemaResponse schema) {
		client().applyChangesToSchema(schema.getUuid(), failingChange()).toCompletable().blockingAwait();
		schema = getSchemaByName(SCHEMA_NAME);
		BranchResponse branch = branch$.blockingGet();
		client().assignBranchSchemaVersions(PROJECT_NAME, branch.getUuid(), schema.toReference()).toCompletable().blockingAwait();
	}

	private void invokeFailingMigration(MicroschemaResponse microschema) {
		client().applyChangesToMicroschema(microschema.getUuid(), failingChange()).toCompletable().blockingAwait();
		microschema = getMicroSchemaByName(microschema.getName());
		BranchResponse branch = branch$.blockingGet();
		client().assignBranchMicroschemaVersions(PROJECT_NAME, branch.getUuid(), microschema.toReference()).toCompletable().blockingAwait();
	}

	private SchemaChangesListModel failingChange() {
		SchemaChangesListModel request = new SchemaChangesListModel();
		SchemaChangeModel operation = new SchemaChangeModel();
		request.getChanges().add(operation);
		operation.setMigrationScript("invalidJavascript");
		operation.setOperation(SchemaChangeOperation.ADDFIELD);
		operation.setProperty("field", "someField");
		operation.setProperty("type", "number");

		return request;
	}

	private void updateSchema(SchemaResponse schema) {
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Arrays.asList(
			new NumberFieldSchemaImpl().setName("name"),
			new StringFieldSchemaImpl().setName("field1")
		));
		client().updateSchema(schema.getUuid(), request)
			.toCompletable().blockingAwait();
	}

	private void updateSchema(MicroschemaResponse schema) {
		MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Arrays.asList(
			new NumberFieldSchemaImpl().setName("name"),
			new StringFieldSchemaImpl().setName("field1")
		));
		client().updateMicroschema(schema.getUuid(), request)
			.toCompletable().blockingAwait();
	}

	private void migrateSchemas() {
		BranchResponse branch = branch$.blockingGet();
		client().migrateBranchSchemas(PROJECT_NAME, branch.getUuid()).toCompletable().blockingAwait();
	}

	private void migrateMicroschemas() {
		BranchResponse branch = branch$.blockingGet();
		client().migrateBranchMicroschemas(PROJECT_NAME, branch.getUuid()).toCompletable().blockingAwait();
	}

	private MicroschemaResponse getMicroSchemaByName(String name) {
		return client().findMicroschemas().toSingle()
			.to(TestUtils::listObservable)
			.filter(schema -> schema.getName().equals(name))
			.blockingFirst();
	}

}

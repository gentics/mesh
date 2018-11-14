package com.gentics.mesh.core.schema;

import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
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

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, clusterMode = false)
public class NodeMigrationFailureRecoveryTest extends AbstractMeshTest {
	private static final String SCHEMA_NAME = "testSchema";

	private Single<ProjectResponse> project$ = Single.defer(() -> client().findProjectByName(PROJECT_NAME).toSingle()).cache();
	private Single<BranchResponse> branch$ = Single.defer(() -> client().findBranches(PROJECT_NAME).toSingle()).map(it -> it.getData().get(0)).cache();

	/**
	 * Test for https://github.com/gentics/mesh/issues/532
	 */
	@Test
	public void testUpdateSchemaAfterFailingMigration() {
		fixPermissions();
		SchemaResponse schema = createSchema();
		createNodes("nodeOne", "nodeOne", "nodeTwo");

		waitForLatestJob(() -> invokeFailingMigration(schema), MigrationStatus.FAILED);
		waitForLatestJob(() -> updateSchema(schema), MigrationStatus.COMPLETED);
		waitForLatestJob(this::migrateSchemas, MigrationStatus.FAILED);
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
		project$.flatMapObservable(project ->
			Observable.fromArray(names)
				.flatMapSingle(name -> {
					NodeCreateRequest request = new NodeCreateRequest();
					request.setParentNode(project.getRootNode());
					request.setLanguage("en");
					request.setSchemaName(SCHEMA_NAME);
					request.getFields().put("name", new StringFieldImpl().setString(name));
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

	private void invokeFailingMigration(SchemaResponse schema) {
		SchemaChangesListModel request = new SchemaChangesListModel();
		SchemaChangeModel operation = new SchemaChangeModel();
		request.getChanges().add(operation);
		operation.setMigrationScript("invalidJavascript");
		operation.setOperation(SchemaChangeOperation.UPDATEFIELD);
		operation.setProperty("field", "name");
		operation.setProperty("type", "number");


		client().applyChangesToSchema(schema.getUuid(), request).toCompletable().blockingAwait();
		schema = getSchemaByName(SCHEMA_NAME);
		BranchResponse branch = branch$.blockingGet();
		client().assignBranchSchemaVersions(PROJECT_NAME, branch.getUuid(), schema.toReference()).toCompletable().blockingAwait();
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

	private void migrateSchemas() {
		BranchResponse branch = branch$.blockingGet();
		client().migrateBranchSchemas(PROJECT_NAME, branch.getUuid()).toCompletable().blockingAwait();
	}

	private SchemaResponse getSchemaByName(String name) {
		return client().findSchemas().toSingle()
			.to(TestUtils::listObservable)
			.filter(schema -> schema.getName().equals(name))
			.blockingFirst();
	}

}

package com.gentics.mesh.test.context.helper;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.apache.commons.lang3.RandomStringUtils;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.ClientHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Test helper for client operation.
 */
public interface ClientHelper extends EventHelper {

	default Completable migrateSchema(String schemaName) {
		return migrateSchema(schemaName, true);
	}

	default Completable migrateSchema(String schemaName, boolean wait) {
		return findSchemaByName(schemaName)
			.flatMapCompletable(schema -> client().updateSchema(schema.getUuid(), addRandomField(schema)).toCompletable())
			.andThen(wait
				? MeshEvent.waitForEvent(meshApi(), MeshEvent.SCHEMA_MIGRATION_FINISHED)
				: Completable.complete());
	}

	default Observable<NodeResponse> findNodesBySchema(String schemaName) {
		return client().findNodes(PROJECT_NAME).toObservable()
			.flatMap(nodes -> Observable.fromIterable(nodes.getData()))
			.filter(node -> node.getSchema().getName().equals(schemaName));
	}

	default SchemaUpdateRequest addRandomField(SchemaResponse schemaResponse) {
		SchemaUpdateRequest request = schemaResponse.toUpdateRequest();
		request.getFields().add(new StringFieldSchemaImpl().setName(RandomStringUtils.randomAlphabetic(10)));
		return request;
	}

	default MicroschemaUpdateRequest addRandomField(MicroschemaResponse schemaResponse) {
		MicroschemaUpdateRequest request = schemaResponse.toRequest();
		request.getFields().add(new StringFieldSchemaImpl().setName(RandomStringUtils.randomAlphabetic(10)));
		return request;
	}

	default Single<SchemaResponse> findSchemaByName(String schemaName) {
		return fetchList(client().findSchemas())
			.filter(schema -> schema.getName().equals(schemaName))
			.singleOrError();
	}

	default <T> Observable<T> fetchList(MeshRequest<? extends ListResponse<T>> request) {
		return request.toObservable().flatMap(response -> Observable.fromIterable(response.getData()));
	}

	/**
	 * Create a new branch
	 * 
	 * @param name
	 *            branch name
	 * @param latest
	 *            true to make branch the latest
	 * @return new branch
	 */
	default HibBranch createBranch(String name, boolean latest) {
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(name);

		if (latest) {
			request.setLatest(latest);
		}

		return createBranch(request);
	}

	/**
	 * Create a branch with the given request
	 * 
	 * @param request
	 *            request
	 * @return new branch
	 */
	default HibBranch createBranch(BranchCreateRequest request) {
		StringBuilder uuid = new StringBuilder();
		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Created branch").hasName(request.getName());
			if (request.isLatest()) {
				assertThat(response).as("Created branch").isLatest();
			} else {
				assertThat(response).as("Created branch").isNotLatest();
			}
			uuid.append(response.getUuid());
		}, COMPLETED, 1);

		// return new branch
		return tx(tx -> {
			return tx.branchDao().findByUuid(project(), uuid.toString());
		});
	}

	default NodeResponse createBinaryNode(String parentNodeUuid) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setSchemaName("binary_content");
		return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
	}

	default Single<NodeResponse> createBinaryContent() {
		String parentUuid = client().findProjects().blockingGet().getData().get(0).getRootNode().getUuid();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid("uuid");
		request.setSchemaName("binary_content");
		request.setParentNodeUuid(parentUuid);
		return client().createNode(PROJECT_NAME, request).toSingle();
	}

	default Single<NodeResponse> createBinaryContent(String uuid) {
		String parentUuid = client().findProjects().blockingGet().getData().get(0).getRootNode().getUuid();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid("uuid");
		request.setSchemaName("binary_content");
		request.setParentNodeUuid(parentUuid);
		return client().createNode(uuid, PROJECT_NAME, request).toSingle();
	}

	default <T> MeshRestClientMessageException adminCall(ClientHandler<T> handler, HttpResponseStatus status, String bodyMessageI18nKey,
		String... i18nParams) {
		return runAsAdmin(() -> {
			return com.gentics.mesh.test.ClientHelper.call(handler, status, bodyMessageI18nKey, i18nParams);
		});
	}

	default <T> T adminCall(ClientHandler<T> handler) {
		return runAsAdmin(() -> {
			return com.gentics.mesh.test.ClientHelper.call(handler);
		});
	}

	default <T> T nonAdminCall(ClientHandler<T> handler) {
		return runAsNonAdmin(() -> {
			return com.gentics.mesh.test.ClientHelper.call(handler);
		});
	}

}

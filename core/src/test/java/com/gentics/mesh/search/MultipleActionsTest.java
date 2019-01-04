package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.madl.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class MultipleActionsTest extends AbstractNodeSearchEndpointTest {
	public static final String SCHEMA_NAME = "content";

	/**
	 * This test does the following: 1. Delete all nodes of a schema 2. Delete that schema 3. Create a new schema 4. Create nodes of that schema 5. Search for
	 * all nodes of that schema
	 *
	 * @throws Exception
	 */
	@Test
	public void testActions() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}
		final int nodeCount = 1;

		AtomicReference<SchemaResponse> newSchema = new AtomicReference<>();
		AtomicReference<NodeReference> rootNodeReference = new AtomicReference<>();

		Completable rootNodeReference$ = getRootNodeReference().doOnSuccess(rootNodeReference::set).cache().toCompletable();
		rootNodeReference$.subscribe();

		Completable actions = getNodesBySchema(SCHEMA_NAME).flatMapCompletable(this::deleteNode).andThen(deleteSchemaByName(SCHEMA_NAME)).andThen(
				createTestSchema()).doOnSuccess(newSchema::set).toCompletable().andThen(rootNodeReference$).andThen(Observable.range(1, nodeCount))
				.flatMapSingle(unused -> createEmptyNode(newSchema.get(), rootNodeReference.get())).ignoreElements();

		NodeListResponse searchResult = actions.andThen(Single.defer(() -> client().searchNodes(getSimpleTermQuery("schema.name.raw", SCHEMA_NAME))
				.toSingle())).blockingGet();
		assertEquals("Check search result after actions", nodeCount, searchResult.getMetainfo().getTotalCount());
	}

	private Observable<NodeResponse> getNodesBySchema(String schemaName) throws JSONException {
		return client().findNodes(PROJECT_NAME, new NodeParametersImpl().setLanguages("en", "de"), new PagingParametersImpl().setPerPage(10000L))
				.toObservable().flatMapIterable(ListResponse::getData).filter(nr -> nr.getSchema().getName().equals(schemaName));
	}

	private Completable deleteNode(NodeResponse node) {
		return client().deleteNode(PROJECT_NAME, node.getUuid()).toCompletable();
	}

	private Completable deleteSchemaByName(String schemaName) throws JSONException {
		SchemaResponse schema = getSchemaByName(schemaName);
		return client().deleteSchema(schema.getUuid()).toCompletable();
	}

	private Single<SchemaResponse> createTestSchema() {
		AtomicReference<SchemaResponse> schemaResponse = new AtomicReference<>();

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.addField(new StringFieldSchemaImpl().setName("testfield1"));
		request.addField(new StringFieldSchemaImpl().setName("testfield2"));

		return client().createSchema(request).toSingle().doOnSuccess(schemaResponse::set).flatMapCompletable(it -> client().assignSchemaToProject(
				PROJECT_NAME, it.getUuid()).toCompletable()).andThen(Single.defer(() -> Single.just(schemaResponse.get())));
	}

	private Single<NodeResponse> createEmptyNode(SchemaResponse schema, NodeReference parentNode) {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(schema.toReference());
		request.setLanguage("en");
		request.setParentNode(parentNode);

		return client().createNode(PROJECT_NAME, request).toSingle();
	}

	private Single<NodeReference> getRootNodeReference() {
		return client().findProjectByName(PROJECT_NAME).toSingle().map(it -> it.getRootNode());
	}

}

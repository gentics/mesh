package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.context.MeshTestSetting;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class MultipleActionsTest extends AbstractNodeSearchEndpointTest {
	public static final String SCHEMA_NAME = "content";

	@Test
	public void testActions() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}
		final int nodeCount = 1;

		AtomicReference<SchemaResponse> newSchema = new AtomicReference<>();
		AtomicReference<NodeReference> rootNodeReference = new AtomicReference<>();

		Completable rootNodeReference$ = getRootNodeReference().doOnSuccess(rootNodeReference::set).cache().toCompletable();
		rootNodeReference$.subscribe();

		Completable actions = getNodesBySchema(SCHEMA_NAME).compose(flatMapCompletable(this::deleteNode)).toCompletable()
				.andThen(deleteSchemaByName(SCHEMA_NAME)).andThen(createTestSchema()).doOnSuccess(newSchema::set).toCompletable()
				.andThen(rootNodeReference$).andThen(Observable.range(1, nodeCount))
				.compose(flatMapSingle(unused -> createEmptyNode(newSchema.get(), rootNodeReference.get()))).toCompletable();

		NodeListResponse searchResult = actions
				.andThen(Single.defer(() -> client().searchNodes(getSimpleTermQuery("schema.name.raw", SCHEMA_NAME)).toSingle())).toBlocking()
				.value();
		assertEquals("Check search result after actions", nodeCount, searchResult.getMetainfo().getTotalCount());
	}

	private Observable<NodeResponse> getNodesBySchema(String schemaName) throws JSONException {
		return client().searchNodes(PROJECT_NAME, getSimpleTermQuery("schema.name.raw", SCHEMA_NAME), new VersioningParameters().draft())
				.toObservable().flatMapIterable(it -> it.getData());
	}

	private Completable deleteNode(NodeResponse node) {
		return client().deleteNode(PROJECT_NAME, node.getUuid()).toCompletable();
	}

	private Completable deleteSchemaByName(String schemaName) throws JSONException {
		return getSchemaByName(schemaName).flatMapCompletable(schema -> client().deleteSchema(schema.getUuid()).toCompletable());
	}

	private Single<SchemaResponse> getSchemaByName(String schemaName) throws JSONException {
		return client().searchSchemas(getSimpleTermQuery("name.raw", schemaName)).toObservable().flatMapIterable(it -> it.getData()).toSingle();
	}

	private Single<SchemaResponse> createTestSchema() {
		AtomicReference<SchemaResponse> schemaResponse = new AtomicReference<>();

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.addField(new StringFieldSchemaImpl().setName("testfield1"));
		request.addField(new StringFieldSchemaImpl().setName("testfield2"));

		return client().createSchema(request).toSingle().doOnSuccess(schemaResponse::set)
				.flatMapCompletable(it -> client().assignSchemaToProject(PROJECT_NAME, it.getUuid()).toCompletable())
				.andThen(Single.defer(() -> Single.just(schemaResponse.get())));
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

	private <T> Observable.Transformer<T, Void> flatMapCompletable(Func1<T, Completable> mapper) {
		return src -> src.map(mapper).toList().flatMap(l -> Completable.merge(l).toObservable());
	}

	private <T, R> Observable.Transformer<T, R> flatMapSingle(Func1<T, Single<R>> mapper) {
		return src -> src.map(mapper).flatMap(it -> it.toObservable());
	}
}

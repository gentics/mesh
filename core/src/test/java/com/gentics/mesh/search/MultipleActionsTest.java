package com.gentics.mesh.search;

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
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class MultipleActionsTest extends AbstractNodeSearchEndpointTest {
	public static final String SCHEMA_NAME = "content";

	/**
	 * This test does the following:
	 * <ol>
	 *     <li>Delete all nodes of a schema</li>
	 *     <li>Delete that schema</li>
	 *     <li>Create a new schema with the same name as the old schema</li>
	 *     <li>Create nodes of that schema</li>
	 *     <li>Search for all nodes of that schema</li>
	 * </ol>
	 *
	 * @throws Exception
	 */
	@Test
	public void testActions() throws Exception {
		recreateIndices();
		final int nodeCount = 1;

		waitForSearchIdleEvent(
			getNodesBySchema(SCHEMA_NAME)
				.flatMapCompletable(this::deleteNode)
				.andThen(deleteSchemaByName(SCHEMA_NAME))
				.andThen(createTestSchema())
				.flatMapObservable(newSchema -> getRootNodeReference()
				.flatMapObservable(rootNodeReference -> Observable.range(1, nodeCount)
				.flatMapSingle(unused -> createEmptyNode(newSchema, rootNodeReference))))
				.ignoreElements()
		);

		NodeListResponse searchResult = client().searchNodes(getSimpleTermQuery("schema.name.raw", SCHEMA_NAME))
				.toSingle().blockingGet();
		assertEquals("Check search result after actions", nodeCount, searchResult.getMetainfo().getTotalCount());
	}

	private Observable<NodeResponse> getNodesBySchema(String schemaName) throws JSONException {
		return client().findNodes(PROJECT_NAME, new NodeParametersImpl().setLanguages("en", "de"), new PagingParametersImpl().setPerPage(10000L))
			.toObservable()
			.flatMapIterable(ListResponse::getData)
			.filter(nr -> nr.getSchema().getName().equals(schemaName));
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

		return client().createSchema(request).toSingle()
			.doOnSuccess(schemaResponse::set)
			.flatMapCompletable(it -> client().assignSchemaToProject(
				PROJECT_NAME, it.getUuid()).toCompletable())
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

}

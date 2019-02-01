package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
@RunWith(VertxUnitRunner.class)
public class NodeEndpointETagReferenceTest extends AbstractMeshTest {

	@Before
	public void createSchemas(TestContext context) {
		createReferenceMicroschema().toCompletable()
			.andThen(createReferenceSchema().toCompletable())
			.subscribe(testSubscriber(context));
	}

	private CompletableObserver testSubscriber(TestContext context) {
		Async async = context.async();
		return new CompletableObserver() {
			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onComplete() {
				async.complete();
			}

			@Override
			public void onError(Throwable e) {
				context.fail(e);
			}
		};
	}

	@Test
	public void testNodeField(TestContext context) {
		assertChangedReference("Check Node Field", (nodeToCheck, nodeToDelete) -> {
			nodeToCheck.getFields().put("node", new NodeFieldImpl().setUuid(nodeToDelete.getUuid()));
		}).subscribe(testSubscriber(context));
	}

	@Test
	public void testNodeListField(TestContext context) {
		assertChangedReference("Check Node List Field", (nodeToCheck, nodeToDelete) -> {
			nodeToCheck.getFields().put("nodeList",
				new NodeFieldListImpl().setItems(Arrays.asList(new NodeFieldListItemImpl().setUuid(nodeToDelete.getUuid()))));
		}).subscribe(testSubscriber(context));
	}

	@Test
	public void testNodeFieldInMicronode(TestContext context) {
		assertChangedReference("Check Node Field in Microschema Field", (nodeToCheck, nodeToDelete) -> {
			MicronodeResponse micronode = new MicronodeResponse();
			micronode.setMicroschema(new MicroschemaReferenceImpl().setName("RefMicroschema"));
			micronode.getFields().put("m-node", new NodeFieldImpl().setUuid(nodeToDelete.getUuid()));
			nodeToCheck.getFields().put("micronode", micronode);
		}).subscribe(testSubscriber(context));
	}

	@Test
	public void testNodeListFieldInMicronode(TestContext context) {
		assertChangedReference("Check Node List Field in Microschema Field", (nodeToCheck, nodeToDelete) -> {
			MicronodeResponse micronode = new MicronodeResponse();
			micronode.setMicroschema(new MicroschemaReferenceImpl().setName("RefMicroschema"));
			micronode.getFields().put("m-nodeList",
				new NodeFieldListImpl().setItems(Arrays.asList(new NodeFieldListItemImpl().setUuid(nodeToDelete.getUuid()))));
			nodeToCheck.getFields().put("micronode", micronode);
		}).subscribe(testSubscriber(context));
	}

	@Test
	public void testNodeFieldInMicronodeList(TestContext context) {
		assertChangedReference("Check Node Field in Microschema Field List", (nodeToCheck, nodeToDelete) -> {
			MicronodeResponse micronode = new MicronodeResponse();
			micronode.setMicroschema(new MicroschemaReferenceImpl().setName("RefMicroschema"));
			micronode.getFields().put("m-node", new NodeFieldImpl().setUuid(nodeToDelete.getUuid()));
			nodeToCheck.getFields().put("micronodeList", new MicronodeFieldListImpl().setItems(Arrays.asList(micronode)));
		}).subscribe(testSubscriber(context));
	}

	@Test
	public void testNodeListFieldinMicronodeList(TestContext context) {
		assertChangedReference("Check Node List Field in Microschema Field List", (nodeToCheck, nodeToDelete) -> {
			MicronodeResponse micronode = new MicronodeResponse();
			micronode.setMicroschema(new MicroschemaReferenceImpl().setName("RefMicroschema"));
			micronode.getFields().put("m-nodeList",
				new NodeFieldListImpl().setItems(Arrays.asList(new NodeFieldListItemImpl().setUuid(nodeToDelete.getUuid()))));
			nodeToCheck.getFields().put("micronodeList", new MicronodeFieldListImpl().setItems(Arrays.asList(micronode)));
		}).subscribe(testSubscriber(context));
	}

	private Single<MicroschemaResponse> createReferenceMicroschema() {
		MicroschemaCreateRequest req = new MicroschemaCreateRequest();
		req.setName("RefMicroschema");
		req.setFields(Arrays.asList(
			new NodeFieldSchemaImpl().setName("m-node"),
			new ListFieldSchemaImpl().setListType("node").setName("m-nodeList")));
		return client().createMicroschema(req).toSingle()
			.flatMap(microSchema -> client().assignMicroschemaToProject(PROJECT_NAME, microSchema.getUuid()).toCompletable()
				.andThen(Single.just(microSchema)));
	}

	private Single<SchemaResponse> createReferenceSchema() {
		SchemaCreateRequest req = new SchemaCreateRequest();
		req.setName("RefSchema");
		req.setFields(Arrays.asList(
			new NodeFieldSchemaImpl().setName("node"),
			new ListFieldSchemaImpl().setListType("node").setName("nodeList"),
			new MicronodeFieldSchemaImpl().setAllowedMicroSchemas("RefMicroschema").setName("micronode"),
			new ListFieldSchemaImpl().setListType("micronode").setName("micronodeList")));
		return client().createSchema(req).toSingle()
			.flatMap(schema -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()).toCompletable()
				.andThen(Single.just(schema)));
	}

	private Single<NodeResponse> createReferenceNode() {
		return getRootUuid().flatMap(rootUuid -> {
			NodeCreateRequest req = new NodeCreateRequest();
			req.setLanguage("en");
			req.setSchemaName("RefSchema");
			req.setParentNodeUuid(rootUuid);
			return client().createNode(PROJECT_NAME, req).toSingle();
		});
	}

	private Single<String> getRootUuid() {
		return client().findProjects().toSingle()
			.map(it -> it.getData().get(0).getRootNode().getUuid());
	}

	private Completable assertChangedReference(String assertionName, BiConsumer<NodeResponse, NodeResponse> fieldChanger) {
		return Single.zip(createReferenceNode(), createReferenceNode(), (nodeToCheck, nodeToDelete) -> {
			fieldChanger.accept(nodeToCheck, nodeToDelete);
			return updateNode(nodeToCheck).toCompletable()
				.andThen(getEtag(nodeToCheck))
				.flatMapCompletable(oldETag -> client().deleteNode(PROJECT_NAME, nodeToDelete.getUuid()).toCompletable()
					.andThen(getEtag(nodeToCheck))
					.doOnSuccess(newEtag -> assertNotEquals(assertionName, oldETag, newEtag))
					.toCompletable());
		}).flatMapCompletable(x -> x);
	}

	private Single<NodeResponse> updateNode(NodeResponse node) {
		NodeUpdateRequest req = new NodeUpdateRequest();
		req.setLanguage(node.getLanguage());
		req.setFields(node.getFields());
		req.setVersion(node.getVersion());
		return client().updateNode(PROJECT_NAME, node.getUuid(), req).toSingle();
	}

	private Single<String> getEtag(NodeResponse node) {
		return client().findNodeByUuid(PROJECT_NAME, node.getUuid()).getResponse()
			.map(response -> response.getHeader(ETAG).orElse(null));
	}
}

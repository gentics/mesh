package com.gentics.mesh.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.bulk.IndexBulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_WITH_INGEST;
@MeshTestSetting(elasticsearch = CONTAINER_WITH_INGEST, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class ElasticSearchProviderTest extends AbstractMeshTest {

	@Test
	public void testProvider() throws IOException {
		ElasticSearchProvider provider = getProvider();
		provider.createIndex(new IndexInfo("test", new JsonObject(), new JsonObject(), "testSchema")).blockingAwait();
		String uuid = UUIDUtil.randomUUID();
		// provider.storeDocument("test", uuid, new JsonObject()).blockingAwait();
		provider.updateDocument("test", uuid, new JsonObject(), true).blockingAwait();

		provider.deleteDocument("test", uuid).blockingAwait();

		// Should not fail if the document is gone since we end result would be the same.
		provider.deleteDocument("test", uuid).blockingAwait();

		provider.deleteIndex("testindex").blockingAwait();

		provider.createIndex(new IndexInfo("testindex", new JsonObject(), new JsonObject(), "testSchema")).blockingAwait();

		provider.createIndex(new IndexInfo("testindex", new JsonObject(), new JsonObject(), "testSchema")).blockingAwait();

		provider.deleteIndex("testindex").blockingAwait();

		// provider.validateCreateViaTemplate(new IndexInfo("test", new JsonObject(), new JsonObject())).blockingAwait();

	}

	@Test
	public void testVersion() {
		ElasticSearchProvider provider = getProvider();
		assertEquals("6.6.1", provider.getVersion());
	}

	@Test
	public void testHasIngestProcessor() {
		SearchClient client = getProvider().getClient();
		assertFalse("A non existing processor name should return false",
				client.hasIngestProcessor("some-fictional-test-processor").blockingGet());

		assertFalse("A non existing processor in a list of processor names should return false",
				client.hasIngestProcessor("some-fictional-test-processor", "attachment").blockingGet());

		assertTrue("Multiple valid processor names should return true",
				client.hasIngestProcessor("append", "attachment").blockingGet());

		assertTrue("The ingest attachment processors should be configured",
				getProvider().hasIngestPipelinePlugin().blockingGet());
	}

	/**
	 * Assert that the ingest pipeline mechanism works as expected.
	 */
	@Test
	public void testIngestPipelineProcessing() {
		ElasticSearchProvider provider = getProvider();
		IndexInfo info = new IndexInfo("test", new JsonObject(), new JsonObject(), "testSchema");
		info.setIngestPipelineSettings(getPipelineConfig(Arrays.asList("data1", "data2")));

		provider.createIndex(info).blockingAwait();
		provider.registerIngestPipeline(info).blockingAwait();

		JsonObject input = new JsonObject();
		input.put("data1", "e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
		input.put("data2", "e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
		input.put("data3", "e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");

		String uuid = UUIDUtil.randomUUID();
		List<BulkEntry> entries = new ArrayList<>();
		entries.add(new IndexBulkEntry("test", uuid, input, true));
		provider.processBulkOld(entries).blockingAwait();

		JsonObject output = provider.getDocument("test", uuid).blockingGet();
		assertEquals("Lorem ipsum dolor sit amet",
			output.getJsonObject("_source").getJsonObject("field").getJsonObject("data1").getString("content"));
		assertEquals("Lorem ipsum dolor sit amet",
			output.getJsonObject("_source").getJsonObject("field").getJsonObject("data2").getString("content"));

	}

	@Test
	public void testClear() throws HttpErrorException {
		ElasticSearchProvider provider = getProvider();
		SearchClient client = (SearchClient) provider.getClient();
		IndexInfo info = new IndexInfo("test", new JsonObject(), new JsonObject(), "testSchema");
		info.setIngestPipelineSettings(getPipelineConfig(Arrays.asList("data1", "data2")));

		client.registerPipeline("othername", getPipelineConfig(Arrays.asList("blub"))).sync();
		provider.createIndex(info).blockingAwait();
		provider.registerIngestPipeline(info).blockingAwait();

		JsonObject pipelines = client.listPipelines().sync();
		assertEquals(8, pipelines.fieldNames().size());

		// Clear the instance
		provider.clear().blockingAwait();

		pipelines = client.listPipelines().sync();
		assertEquals(1, pipelines.fieldNames().size());

		provider.deregisterPipeline("notfound").blockingAwait();
	}

	@Test
	public void testPrefixHandling() {
		assertEquals("mesh-", getProvider().installationPrefix());

		String fullIndex = "mesh-node-blar";
		assertEquals("node-blar", getProvider().removePrefix(fullIndex));
	}

	/**
	 * Assert that the indices in the request path never exceed 4096 bytes. Otherwise the request would fail.
	 */
	@Test
	public void testIndexSegmentation() {
		ElasticSearchProvider provider = getProvider();
		List<String> indices = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			StringBuilder builder = new StringBuilder();
			builder.append("mesh-test");
			for (int e = 0; e < 4; e++) {
				builder.append(UUIDUtil.randomUUID());
			}
			String name = builder.toString();
			indices.add(name);
			provider.createIndex(new IndexInfo(name, new JsonObject(), new JsonObject(), "testSchema")).blockingAwait();
		}

		String names[] = indices.stream().toArray(String[]::new);
		provider.refreshIndex(names).blockingAwait();
		provider.refreshIndex(names).blockingAwait();
		provider.clear().blockingAwait();
		provider.clear().blockingAwait();
	}

	@Test
	public void testConcurrencyConflictError() {
		ElasticSearchProvider provider = getProvider();
		provider.createIndex(new IndexInfo("test", new JsonObject(), new JsonObject(), "testSchema")).blockingAwait();
		provider.storeDocument("test", "1", new JsonObject().put("value", 0)).blockingAwait();
		Observable.range(1, 2000).flatMapCompletable(i -> provider.updateDocument("test", "1", new JsonObject().put("value", i), false))
			.blockingAwait();
	}

	private JsonObject getPipelineConfig(List<String> fields) {
		JsonObject config = new JsonObject();
		config.put("description", "Extract attachment information");

		JsonArray processors = new JsonArray();
		for (String field : fields) {
			JsonObject processor = new JsonObject();
			JsonObject settings = new JsonObject();
			settings.put("field", field);
			settings.put("target_field", "field." + field);
			settings.put("ignore_missing", true);
			processor.put("attachment", settings);
			processors.add(processor);
		}

		config.put("processors", processors);
		return config;
	}
}

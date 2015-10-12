package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.failingLatch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
public abstract class AbstractSearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	protected SearchVerticle searchVerticle;

	@Autowired
	protected SearchProvider searchProvider;

	@After
	public void resetElasticSearch() {
		searchProvider.reset();
	}

	@BeforeClass
	@AfterClass
	public static void clean() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	protected String getSimpleQuery(String text) throws JSONException {
		QueryBuilder qb = QueryBuilders.queryStringQuery(text);
		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(qb.toString()));
		return request.toString();
	}

	protected String getSimpleTermQuery(String key, String value) throws JSONException {
		QueryBuilder qb = QueryBuilders.termQuery(key, value);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(qb);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(bqb.toString()));
		return request.toString();
	}

	protected void fullIndex() throws InterruptedException {
		boot.meshRoot().getSearchQueue().addFullIndex();
		CountDownLatch latch = new CountDownLatch(1);
		boot.meshRoot().getSearchQueue().processAll(rh -> {
			latch.countDown();
		});
		failingLatch(latch, 30);
	}

	abstract public void testDocumentDeletion() throws Exception;

	abstract public void testDocumentCreation() throws Exception;

	abstract public void testDocumentUpdate() throws Exception;

}

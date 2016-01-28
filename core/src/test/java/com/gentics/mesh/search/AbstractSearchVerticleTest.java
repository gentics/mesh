package com.gentics.mesh.search;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.BaseFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
public abstract class AbstractSearchVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchVerticleTest.class);

	@Autowired
	protected SearchVerticle searchVerticle;

	@Autowired
	protected SearchProvider searchProvider;

	@Autowired
	private IndexHandlerRegistry registry;

	@Before
	public void initElasticSearch() {
		for (IndexHandler handler : registry.getHandlers()) {
			handler.init().toBlocking().single();
		}
	}

	@After
	public void resetElasticSearch() {
		searchProvider.reset();
		for (IndexHandler handler : registry.getHandlers()) {
			handler.init().toBlocking().single();
		}
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
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	protected String getSimpleTermQuery(String key, String value) throws JSONException {
		QueryBuilder qb = QueryBuilders.termQuery(key, value);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(qb);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(bqb.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	protected String getSimpleWildCardQuery(String key, String value) throws JSONException {
		QueryBuilder qb = QueryBuilders.wildcardQuery(key, value);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(qb);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(bqb.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	protected String getRangeQuery(String fieldName, double from, double to) throws JSONException {
		RangeFilterBuilder range = FilterBuilders.rangeFilter(fieldName).gte(from).lte(to);
		return filterWrapper(range);
	}

	private String filterWrapper(BaseFilterBuilder filter) throws JSONException {
		JSONObject request = new JSONObject();
		request.put("filter", new JSONObject(filter.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	protected void fullIndex() throws InterruptedException {
		boot.meshRoot().getSearchQueue().addFullIndex();
		boot.meshRoot().getSearchQueue().processAll();
	}

}

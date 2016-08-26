package com.gentics.mesh.search;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractSearchVerticleTest extends AbstractIsolatedRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchVerticleTest.class);

	// protected SearchProvider searchProvider;
	//
	// private IndexHandlerRegistry registry;
	//
	// private NodeIndexHandler nodeIndexHandler;

	@Before
	public void setupES() {
		init();
		initWithSearch();
	}
	
	
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		for (IndexHandler handler : meshDagger.indexHandlerRegistry().getHandlers()) {
			handler.init().await();
		}
	}

	@After
	public void resetElasticSearch() {
		// searchProvider.reset();
		searchProvider.clear();
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
		RangeQueryBuilder range = QueryBuilders.rangeQuery(fieldName).from(from).to(to);
		return "{ \"query\": " + range.toString() + "}";
	}

	protected void fullIndex() throws InterruptedException, InvalidArgumentException {
		Project project = project();
		for (Release release : project.getReleaseRoot().findAll()) {
			for (SchemaContainerVersion version : release.findAllSchemaVersions()) {
				String type = version.getName() + "-" + version.getVersion();
				String drafIndex = "node-" + project.getUuid() + "-" + release.getUuid() + "-draft";
				log.debug("Creating schema mapping for index {" + drafIndex + "}");
				meshDagger.nodeIndexHandler().updateNodeIndexMapping(drafIndex, type, version.getSchema()).await();

				String publishIndex = "node-" + project.getUuid() + "-" + release.getUuid() + "-published";
				log.debug("Creating schema mapping for index {" + publishIndex + "}");
				meshDagger.nodeIndexHandler().updateNodeIndexMapping(publishIndex, type, version.getSchema()).await();
			}
		}

		boot.meshRoot().getSearchQueue().addFullIndex();
		boot.meshRoot().getSearchQueue().processAll();
	}

}

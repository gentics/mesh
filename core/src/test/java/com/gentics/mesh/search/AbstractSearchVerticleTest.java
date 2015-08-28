package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
public abstract class AbstractSearchVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchVerticleTest.class);

	@Autowired
	protected SearchVerticle searchVerticle;

	@Autowired
	protected ElasticSearchProvider elasticSearchProvider;

	@After
	public void resetElasticSearch() {
		elasticSearchProvider.reset();
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

	protected String getSimpleTermQuery(String key, String value) {
		QueryBuilder qb = QueryBuilders.termQuery(key, value);
		String json = "{";
		json += "	 \"query\":" + qb.toString();
		json += "	}";
		return json;
	}

	protected void setupFullIndex() throws InterruptedException {
		try (Trx tx = db.trx()) {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			SearchQueueBatch batch = searchQueue.createBatch("0");
			for (Node node : boot.nodeRoot().findAll()) {
				batch.addEntry(node, CREATE_ACTION);
			}
			for (Project project : boot.projectRoot().findAll()) {
				batch.addEntry(project, CREATE_ACTION);
			}
			for (User user : boot.userRoot().findAll()) {
				batch.addEntry(user, CREATE_ACTION);
			}
			for (Role role : boot.roleRoot().findAll()) {
				batch.addEntry(role, CREATE_ACTION);
			}
			for (Group group : boot.groupRoot().findAll()) {
				batch.addEntry(group, CREATE_ACTION);
			}
			for (Tag tag : boot.tagRoot().findAll()) {
				batch.addEntry(tag, CREATE_ACTION);
			}
			for (TagFamily tagFamily : boot.tagFamilyRoot().findAll()) {
				batch.addEntry(tagFamily, CREATE_ACTION);
			}
			for (SchemaContainer schema : boot.schemaContainerRoot().findAll()) {
				batch.addEntry(schema, CREATE_ACTION);
			}
			//TODO add support for microschemas
			//			for (Microschema microschema : boot.microschemaContainerRoot().findAll()) {
			//				searchQueue.put(microschema, CREATE_ACTION);
			//			}
			log.debug("Search Queue size:" + searchQueue.getSize());
			tx.success();
		}

		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await(20, TimeUnit.SECONDS);
	}

}

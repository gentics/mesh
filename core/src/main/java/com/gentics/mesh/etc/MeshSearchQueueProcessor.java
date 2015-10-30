package com.gentics.mesh.etc;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Configuration
public class MeshSearchQueueProcessor {

	private static final Logger log = LoggerFactory.getLogger(MeshSearchQueueProcessor.class);

	@Autowired
	private Database database;

	@Autowired
	private SearchProvider searchProvider;

	@Autowired
	private BootstrapInitializer initalizer;

	@PostConstruct
	public void process() throws InterruptedException {
		database.noTrx(tx -> {
			long start = System.currentTimeMillis();
			try {
				initalizer.meshRoot().getSearchQueue().processAll();
			} catch (Exception e) {
				log.error("Error during search queue processing", e);
			}
			long duration = System.currentTimeMillis() - start;
			log.info("Completed processing of remaining search queue entries. Processing took {" + duration + "} ms.");
		});
	}
}

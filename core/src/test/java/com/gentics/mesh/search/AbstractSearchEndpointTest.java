package com.gentics.mesh.search;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.AbstractRestEndpointTest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractSearchEndpointTest extends AbstractRestEndpointTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchEndpointTest.class);

	@BeforeClass
	public static void initMesh() throws Exception {
		init(true);
		initDagger(false);
	}

	@Before
	public void setupHandlers() throws Exception {
		// We need to call init() again in order create missing indices for the created test data
		for (IndexHandler handler : meshDagger.indexHandlerRegistry().getHandlers()) {
			handler.init().await();
		}
	}

	@After
	public void resetElasticSearch() {
		searchProvider.clear();
	}

	@BeforeClass
	@AfterClass
	public static void clean() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	/**
	 * Drop all indices and create a new index using the current data.
	 * 
	 * @throws Exception
	 */
	protected void recreateIndices() throws Exception {
		// We potentially modified existing data thus we need to drop all indices and create them and reindex all data
		searchProvider.clear();
		setupHandlers();
		IndexHandlerRegistry registry = MeshInternal.get().indexHandlerRegistry();
		for (IndexHandler handler : registry.getHandlers()) {
			handler.reindexAll().await();
		}
	}

}

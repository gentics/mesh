package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import io.reactivex.functions.Action;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;

import static com.gentics.mesh.util.RxUtil.NOOP;

/**
 * A helper that provides various methods for event handlers.
 */
public class MeshHelper {
	private static final Logger log = LoggerFactory.getLogger(MeshHelper.class);

	private final Database db;
	private final MeshOptions options;
	private final BootstrapInitializer boot;
	private final SearchProvider searchProvider;

	@Inject
	public MeshHelper(Database db, MeshOptions options, BootstrapInitializer boot, SearchProvider searchProvider) {
		this.db = db;
		this.options = options;
		this.boot = boot;
		this.searchProvider = searchProvider;
	}

	private String prefixIndexName(String index) {
		String prefix = options.getSearchOptions().getPrefix();
		return prefix == null
			? index
			: prefix + index;
	}

	/**
	 * Creates a {@link CreateDocumentRequest} and prefixes the index with the configured prefix.
	 * @param index
	 * @param id
	 * @param doc
	 * @return
	 */
	public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc) {
		return new CreateDocumentRequest(index, prefixIndexName(index), id, doc, NOOP, searchProvider.hasIngestPipelinePlugin());
	}

	/**
	 * Creates a {@link CreateDocumentRequest} and prefixes the index with the configured prefix.
	 * @param index
	 * @param id
	 * @param doc
	 * @param onComplete
	 * @return
	 */
	public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc, Action onComplete) {
		return new CreateDocumentRequest(index, prefixIndexName(index), id, doc, onComplete, searchProvider.hasIngestPipelinePlugin());
	}

	/**
	 * Creates a {@link UpdateDocumentRequest} and prefixes the index with the configured prefix.
	 * @param index
	 * @param id
	 * @param doc
	 * @return
	 */
	public UpdateDocumentRequest updateDocumentRequest(String index, String id, JsonObject doc) {
		return new UpdateDocumentRequest(index, prefixIndexName(index), id, doc);
	}

	/**
	 * Creates a {@link DeleteDocumentRequest} and prefixes the index with the configured prefix.
	 * @param index
	 * @param id
	 * @return
	 */
	public DeleteDocumentRequest deleteDocumentRequest(String index, String id) {
		return new DeleteDocumentRequest(index, prefixIndexName(index), id);
	}

	/**
	 * Creates a {@link DeleteDocumentRequest} and prefixes the index with the configured prefix.
	 * @param index
	 * @param id
	 * @param onComplete
	 * @return
	 */
	public DeleteDocumentRequest deleteDocumentRequest(String index, String id, Action onComplete) {
		return new DeleteDocumentRequest(index, prefixIndexName(index), id, onComplete);
	}

	public Database getDb() {
		return db;
	}

	public BootstrapInitializer getBoot() {
		return boot;
	}

}

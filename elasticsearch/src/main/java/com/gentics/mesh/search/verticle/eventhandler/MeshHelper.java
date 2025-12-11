package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.util.RxUtil.NOOP;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.functions.Action;
import io.vertx.core.json.JsonObject;

/**
 * A helper that provides various methods for event handlers.
 */
public class MeshHelper {

	private final Database db;
	private final MeshOptions options;
	private final BootstrapInitializer boot;

	@Inject
	public MeshHelper(Database db, MeshOptions options, BootstrapInitializer boot) {
		this.db = db;
		this.options = options;
		this.boot = boot;
	}

	private String prefixIndexName(String index) {
		String prefix = options.getSearchOptions().getPrefix();
		return prefix == null
			? index
			: prefix + index;
	}

	/**
	 * Creates a {@link CreateDocumentRequest} and prefixes the index with the configured prefix.
	 * 
	 * @param index
	 * @param id
	 * @param doc
	 * @param mode
	 * @return
	 */
	public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc, Compliance compliance) {
		return new CreateDocumentRequest(index, prefixIndexName(index), id, doc, compliance, NOOP);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} and prefixes the index with the configured prefix.
	 * 
	 * @param index
	 * @param id
	 * @param doc
	 * @param mode
	 * @param onComplete
	 * @return
	 */
	public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc, Compliance compliance, Action onComplete) {
		return new CreateDocumentRequest(index, prefixIndexName(index), id, doc, compliance, onComplete);
	}

	/**
	 * Creates a {@link UpdateDocumentRequest} and prefixes the index with the configured prefix.
	 * 
	 * @param index
	 * @param id
	 * @param doc
	 * @return
	 */
	public UpdateDocumentRequest updateDocumentRequest(String index, String id, JsonObject doc, Compliance compliance) {
		return new UpdateDocumentRequest(index, prefixIndexName(index), id, doc, compliance);
	}

	/**
	 * Creates a {@link DeleteDocumentRequest} and prefixes the index with the configured prefix.
	 * 
	 * @param index
	 * @param id
	 * @param mode
	 * @return
	 */
	public DeleteDocumentRequest deleteDocumentRequest(String index, String id, Compliance compliance) {
		return new DeleteDocumentRequest(index, prefixIndexName(index), id, compliance);
	}

	/**
	 * Creates a {@link DeleteDocumentRequest} and prefixes the index with the configured prefix.
	 * 
	 * @param index
	 * @param id
	 * @param mode
	 * @param onComplete
	 * @return
	 */
	public DeleteDocumentRequest deleteDocumentRequest(String index, String id, Compliance compliance, Action onComplete) {
		return new DeleteDocumentRequest(index, prefixIndexName(index), id, compliance, onComplete);
	}

	public Database getDb() {
		return db;
	}

	public BootstrapInitializer getBoot() {
		return boot;
	}

}

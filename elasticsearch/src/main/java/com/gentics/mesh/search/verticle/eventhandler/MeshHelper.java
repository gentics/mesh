package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;

public class MeshHelper {
	private static final Logger log = LoggerFactory.getLogger(MeshHelper.class);

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

	public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc) {
		return new CreateDocumentRequest(index, prefixIndexName(index), id, doc);
	}

	public DeleteDocumentRequest deleteDocumentRequest(String index, String id) {
		return new DeleteDocumentRequest(index, prefixIndexName(index), id);
	}

	public Database getDb() {
		return db;
	}

	public BootstrapInitializer getBoot() {
		return boot;
	}
}

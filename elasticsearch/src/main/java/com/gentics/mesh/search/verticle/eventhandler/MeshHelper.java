package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.Transformer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class MeshHelper {
	private static final Logger log = LoggerFactory.getLogger(MeshHelper.class);

	private final Database db;
	private final MeshOptions options;

	@Inject
	public MeshHelper(Database db, MeshOptions options) {
		this.db = db;
		this.options = options;
	}

	public String prefixIndexName(String index) {
		String prefix = options.getSearchOptions().getPrefix();
		return prefix == null
			? index
			: prefix + index;
	}

	public Database getDb() {
		return db;
	}

	public <T extends MeshCoreVertex<? extends RestModel, T>> Optional<JsonObject> getDocument(MeshEntity<T> entity, String uuid) {
		RootVertex<T> rootVertex = entity.getRootVertex();
		Transformer<T> transformer = entity.getTransformer();

		return db.tx(tx -> {
			T element = rootVertex.findByUuid(uuid);

			if (element != null) {
				log.trace(String.format("Transforming document with uuid {%s} and transformer {%s}", uuid, transformer.getClass().getSimpleName()));
				return Optional.of(transformer.toDocument(element));
			} else {
				log.warn(String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()));
				return Optional.empty();
			}
		});
	}

}

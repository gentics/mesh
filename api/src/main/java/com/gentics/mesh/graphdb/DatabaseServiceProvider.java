package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import com.syncleus.ferma.FramedTransactionalGraph;

public interface DatabaseServiceProvider {

	FramedTransactionalGraph getFramedGraph(JsonObject settings);

}

package com.gentics.mesh.graphdb;

import java.io.IOException;

import io.vertx.core.json.JsonObject;

import com.syncleus.ferma.FramedTransactionalGraph;

public interface DatabaseServiceProvider {

	FramedTransactionalGraph getFramedGraph(JsonObject settings) throws IOException;

}

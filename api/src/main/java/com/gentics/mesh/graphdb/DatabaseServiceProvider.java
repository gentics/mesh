package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import java.io.IOException;

import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public interface DatabaseServiceProvider {

	FramedThreadedTransactionalGraph getFramedGraph(JsonObject settings) throws IOException;

}

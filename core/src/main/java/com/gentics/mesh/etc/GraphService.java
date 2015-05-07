package com.gentics.mesh.etc;

import io.vertx.core.json.JsonObject;

import org.neo4j.graphdb.GraphDatabaseService;

import com.gentics.mesh.etc.config.MeshNeo4jConfiguration;

public interface GraphService {

    public void initialize(MeshNeo4jConfiguration configuration) throws Exception;

    public GraphDatabaseService getGraphDatabaseService();

    public JsonObject query(JsonObject request) throws Exception;

    public void shutdown();
}

package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, inMemoryDB = false, testSize = FULL, startServer = true, clusterMode = true)
public class NodeMigrationEndpointClusteredTest extends NodeMigrationEndpointTest {

}

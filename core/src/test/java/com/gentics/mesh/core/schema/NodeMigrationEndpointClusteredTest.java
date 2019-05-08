package com.gentics.mesh.core.schema;


import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, inMemoryDB = false, testSize = FULL, startServer = true, clusterMode = true, monitoring = false)
public class NodeMigrationEndpointClusteredTest extends NodeMigrationEndpointTest {

}

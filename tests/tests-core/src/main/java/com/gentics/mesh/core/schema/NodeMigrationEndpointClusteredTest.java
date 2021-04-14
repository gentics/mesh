package com.gentics.mesh.core.schema;


import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, inMemoryDB = false, testSize = FULL, startServer = true, clusterMode = true, monitoring = false)
public class NodeMigrationEndpointClusteredTest extends NodeMigrationEndpointTest {

}

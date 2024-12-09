package com.gentics.mesh.core.schema;


import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.ClusterTests;

@Category(ClusterTests.class)
@MeshTestSetting(elasticsearch = TRACKING, inMemoryDB = false, testSize = FULL, startServer = true, clusterMode = true, monitoring = false, clusterInstances = 1, clusterName = "NodeMigrationEndpointClusteredTest")
public class NodeMigrationEndpointClusteredTest extends NodeMigrationEndpointTest {

}

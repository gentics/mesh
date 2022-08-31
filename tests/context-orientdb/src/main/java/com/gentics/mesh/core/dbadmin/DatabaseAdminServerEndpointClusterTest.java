package com.gentics.mesh.core.dbadmin;

import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.PROJECT;

import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = true, inMemoryDB = true, clusterMode = true)
public class DatabaseAdminServerEndpointClusterTest extends DatabaseAdminServerEndpointTest{

}

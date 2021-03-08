package com.gentics.mesh.plugin;

import static com.gentics.mesh.test.TestSize.PROJECT;

import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Run the plugin tests in clustered mode with a single instance.
 */
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = false, clusterMode = true)
public class PluginManagerClusterTest extends PluginManagerTest {

}

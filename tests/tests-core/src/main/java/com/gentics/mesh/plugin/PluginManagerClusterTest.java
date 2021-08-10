package com.gentics.mesh.plugin;

import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.PluginTests;

/**
 * Run the plugin tests in clustered mode with a single instance.
 */
@Category(PluginTests.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = false, clusterMode = true)
public class PluginManagerClusterTest extends PluginManagerTest {

}

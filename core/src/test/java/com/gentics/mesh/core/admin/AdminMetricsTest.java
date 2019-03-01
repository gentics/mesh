package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminMetricsTest extends AbstractMeshTest {

	@Test
	public void testLoadMetrics() {
		for (int i = 0; i < 10; i++) {
			call(() -> client().me());
		}
		String metrics = call(() -> client().metrics());
		System.out.println(metrics);
	}

}

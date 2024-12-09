package com.gentics.mesh.distributed.coordinator;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.distributed.AbstractMeshClusteringTest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.ClusterTests;
import com.gentics.mesh.test.context.MeshTestContext.MeshTestInstance;

/**
 * Test cases for the {@link MasterElector}
 */
@Category(ClusterTests.class)
@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = true, clusterName = "MasterElectorTest")
public class MasterElectorTest extends AbstractMeshClusteringTest {
	/**
	 * Test getting the cluster status from all cluster instances.
	 * The results are expected to
	 * <ol>
	 * <li>Contain all instances</li>
	 * <li>All show exactly one MASTER</li>
	 * </ol>
	 */
	@Test
	public void testClusterStatus() {
		for (MeshTestInstance instance : testContext.getInstances()) {
			String name = instance.getOptions().getNodeName();
			ClusterStatusResponse status = call(() -> instance.getHttpClient().clusterStatus());
			assertThat(status.getInstances().stream().map(ClusterInstanceInfo::getName).collect(Collectors.toSet()))
				.as("Instances returned by " + name).containsOnlyElementsOf(instancePerName.keySet());
			assertThat(status.getInstances().stream().map(ClusterInstanceInfo::getRole).collect(Collectors.toList()))
				.as("Instance roles returned by " + name).containsOnlyOnce("MASTER");
		}
	}

	/**
	 * Test getting the coordination master from all instances.
	 * The results are expected to all show the same master instance.
	 */
	@Test
	public void testMasterInfo() {
		String masterName = null;
		for (MeshTestInstance instance : testContext.getInstances()) {
			String name = instance.getOptions().getNodeName();
			CoordinatorMasterResponse coordinationMaster = call(() -> instance.getHttpClient().loadCoordinationMaster());
			assertThat(coordinationMaster.getName()).as("Coordination master returned by " + name).isNotEmpty();
			if (masterName != null) {
				assertThat(coordinationMaster.getName()).as("Coordination master returned by " + name)
						.isEqualTo(masterName);
			} else {
				masterName = coordinationMaster.getName();
			}
		}
	}

	/**
	 * Test setting the master instance.
	 */
	@Test
	public void testSetMaster() {
		for (Entry<String, MeshTestInstance> entry : instancePerName.entrySet()) {
			String masterName = entry.getKey();
			MeshTestInstance instance = entry.getValue();

			call(() -> instance.getHttpClient().setCoordinationMaster());

			for (MeshTestInstance inst : testContext.getInstances()) {
				String name = inst.getOptions().getNodeName();
				CoordinatorMasterResponse coordinationMaster = call(() -> inst.getHttpClient().loadCoordinationMaster());
				assertThat(coordinationMaster.getName()).as("Coordination master returned by " + name).isEqualTo(masterName);
			}
		}
	}
}

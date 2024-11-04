package com.gentics.mesh.distributed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContext.MeshTestInstance;

/**
 * Abstract base class for clustering tests.
 */
public abstract class AbstractMeshClusteringTest {
	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	/**
	 * Map of mesh instances per node name
	 */
	protected static Map<String, MeshTestInstance> instancePerName = new HashMap<>();

	@Before
	public void setup() {
		for (MeshTestInstance instance : testContext.getInstances()) {
			instance.getHttpClient().setLogin("admin", "admin").login().blockingGet();

			instancePerName.put(instance.getOptions().getNodeName(), instance);
		}
	}

	/**
	 * Get the instance with given index.
	 * @param index instance index
	 * @return instance
	 */
	protected MeshTestInstance getInstance(int index) {
		assertThat(testContext.getInstances()).areAtLeast(index,
				new Condition<MeshTestInstance>(inst -> inst != null, "not null"));
		return testContext.getInstances().get(index);
	}

	/**
	 * Get the name of the instance with given index
	 * @param index instance index
	 * @return instance name
	 */
	protected String getInstanceName(int index) {
		assertThat(testContext.getInstances()).areAtLeast(index,
				new Condition<MeshTestInstance>(inst -> inst != null, "not null"));
		return testContext.getInstances().get(index).getOptions().getNodeName();
	}
}

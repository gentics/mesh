package com.gentics.mesh.changelog.highlevel.change;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = false)
public class RestructureWebrootIndexTest extends AbstractMeshTest {

	@Test
	public void testChange() {
		tx(() -> {
			RestructureWebrootIndex index = new RestructureWebrootIndex(db());
			index.apply();
		});
	}
}

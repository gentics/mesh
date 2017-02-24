package com.gentics.mesh.core.rest;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class MeshLocalClientTest extends AbstractMeshTest {

	@Test
	public void testClientParameterHandling() {
		String newsNodeUuid = db().noTx(() -> folder("news").getUuid());
		MeshAuthUser user = db().noTx(() -> {
			return MeshInternal.get().boot().meshRoot().getUserRoot().findMeshAuthUserByUsername(user().getUsername());
		});
		meshDagger().meshLocalClientImpl().setUser(user);
		NodeResponse response = call(
				() -> meshDagger().meshLocalClientImpl().findNodeByUuid(PROJECT_NAME, newsNodeUuid, new NodeParameters().setLanguages("de")));
		assertEquals("Neuigkeiten", response.getFields().getStringField("name").getString());
	}
}

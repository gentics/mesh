package com.gentics.mesh.core.rest;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MeshLocalClientTest extends AbstractMeshTest {

	@Test
	public void testClientParameterHandling() {
		String newsNodeUuid = db().tx(() -> folder("news").getUuid());
		MeshAuthUser user = db().tx(tx -> { return tx.userDao().findMeshAuthUserByUsername(user().getUsername()); });
		meshDagger().meshLocalClientImpl().setUser(user);
		NodeResponse response = call(
				() -> meshDagger().meshLocalClientImpl().findNodeByUuid(PROJECT_NAME, newsNodeUuid, new NodeParametersImpl().setLanguages("de")));
		assertEquals("Neuigkeiten", response.getFields().getStringField("slug").getString());
	}
}

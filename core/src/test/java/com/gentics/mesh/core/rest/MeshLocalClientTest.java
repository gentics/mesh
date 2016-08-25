package com.gentics.mesh.core.rest;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class MeshLocalClientTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.nodeVerticle());
		return list;
	}

	@Test
	public void testClientParameterHandling() {
		String newsNodeUuid = db.noTx(() -> folder("news").getUuid());
		MeshAuthUser user = db.noTx(() -> {
			return MeshRoot.getInstance().getUserRoot().findMeshAuthUserByUsername(user().getUsername());
		});
		meshDagger.meshLocalClientImpl().setUser(user);
		NodeResponse response = call(() -> meshDagger.meshLocalClientImpl().findNodeByUuid(PROJECT_NAME, newsNodeUuid, new NodeParameters().setLanguages("de")));
		assertEquals("Neuigkeiten", response.getFields().getStringField("name").getString());
	}
}

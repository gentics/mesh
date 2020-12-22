package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;

public class MeshMeshIntegrationTest {

	@Test
	public void testMeshMesh() throws Exception {
		MeshOptions optionA = new MeshOptions().setNodeName("A");
		optionA.getAuthenticationOptions().setKeystorePassword("ABC");
		optionA.getSearchOptions().setStartEmbedded(false);
		optionA.getSearchOptions().setUrl(null);
		optionA.getStorageOptions().setDirectory(null);
		optionA.getHttpServerOptions().setPort(8081);
		optionA.getMonitoringOptions().setEnabled(false);
		optionA.getMonitoringOptions().setPort(8082);
		Mesh meshA = Mesh.create(optionA);

		MeshOptions optionB = new MeshOptions().setNodeName("B");
		optionB.getAuthenticationOptions().setKeystorePassword("ABC");
		optionB.getSearchOptions().setStartEmbedded(false);
		optionB.getSearchOptions().setUrl(null);
		optionB.getStorageOptions().setDirectory(null);
		optionB.getHttpServerOptions().setPort(8083);
		optionB.getMonitoringOptions().setEnabled(false);
		optionB.getMonitoringOptions().setPort(8084);
		Mesh meshB = Mesh.create(optionB);

		assertNotEquals(meshA, meshB);

		meshA.run(false);

		meshB.run(false);

		System.out.println("Done");

		MeshComponent meshInternalA = meshA.internal();
		MeshComponent meshInternalB = meshB.internal();
		assertNotEquals(meshInternalA, meshInternalB);
		assertNotEquals(meshInternalA.boot(), meshInternalB.boot());
		meshInternalA.database().tx(() -> {
			System.out.println(meshInternalA.boot().userRoot().getUuid());
			System.out.println("Admin in A: " + meshInternalA.boot().userDao().findByName("admin").getUuid());
		});
		meshInternalB.database().tx(() -> {
			System.out.println(meshInternalB.boot().userRoot().getUuid());
			System.out.println("Admin in B: " + meshInternalB.boot().userDao().findByName("admin").getUuid());
		});

		meshA.shutdown();
		meshB.shutdown();
	}
}

package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.context.MeshOptionsTypeAwareContext;

public abstract class MeshMeshIntegrationTest<T extends MeshOptions> implements MeshOptionsTypeAwareContext<T> {

	@Test
	public void testMeshMesh() throws Exception {
		T optionA = getOptions();
		optionA.setNodeName("A");
		optionA.getAuthenticationOptions().setKeystorePassword("ABC");
		optionA.getSearchOptions().setStartEmbedded(false);
		optionA.getSearchOptions().setUrl(null);
		optionA.getHttpServerOptions().setPort(8081);
		optionA.getMonitoringOptions().setEnabled(false);
		optionA.getMonitoringOptions().setPort(8082);		
		setupOptions(optionA);		
		Mesh meshA = Mesh.create(optionA);

		T optionB = getOptions();
		optionB.setNodeName("B");
		optionB.getAuthenticationOptions().setKeystorePassword("ABC");
		optionB.getSearchOptions().setStartEmbedded(false);
		optionB.getSearchOptions().setUrl(null);
		optionB.getHttpServerOptions().setPort(8083);
		optionB.getMonitoringOptions().setEnabled(false);
		optionB.getMonitoringOptions().setPort(8084);
		setupOptions(optionB);
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
	
	abstract void setupOptions(T options);
}

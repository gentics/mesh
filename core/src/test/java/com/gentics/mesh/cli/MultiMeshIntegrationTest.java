package com.gentics.mesh.cli;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;

@Ignore
public class MultiMeshIntegrationTest {

	public static final int INSTANCE_COUNT = 10;

	@Test
	public void testMeshMesh() throws Exception {

		List<Mesh> meshes = new ArrayList<>();
		for (int i = 0; i <= INSTANCE_COUNT; i++) {
			MeshOptions option = new MeshOptions().setNodeName("M" + i);
			option.getAuthenticationOptions().setKeystorePassword("ABC");
			option.getSearchOptions().setStartEmbedded(false);
			option.getSearchOptions().setUrl(null);
			option.getStorageOptions().setDirectory("data/m" + i);
			option.getHttpServerOptions().setPort(8000 + i);
			option.getMonitoringOptions().setEnabled(false);
			option.getMonitoringOptions().setPort(8500 + i);
			Mesh mesh = Mesh.create(option);
			mesh.run(false);
			System.out.println("Done");
			MeshComponent meshInternal = mesh.internal();
			meshInternal.database().tx(tx -> {
				UserDaoWrapper userDao = tx.data().userDao();
				System.out.println(userDao.getUuid());
				System.out.println("Admin: " + userDao.findByName("admin").getUuid());
			});
			meshes.add(mesh);

		}

		System.out.println("Press any key to shutdown");
		System.in.read();
		System.out.println("Done.. Shutting them down..");
		for (Mesh mesh : meshes) {
			System.out.println("Shutdown " + mesh.getOptions().getNodeName());
			mesh.shutdown();
		}
	}
}

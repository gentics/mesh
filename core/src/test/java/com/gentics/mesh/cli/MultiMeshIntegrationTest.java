package com.gentics.mesh.cli;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshOptionsTypeAwareContext;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;

public abstract class MultiMeshIntegrationTest<T extends MeshOptions> implements MeshOptionsTypeAwareContext<T> {

	public static final int INSTANCE_COUNT = 10;

	@Test
	public void testMeshMesh() throws Exception {

		List<Mesh> meshes = new ArrayList<>();
		for (int i = 0; i <= INSTANCE_COUNT; i++) {
			T option = getOptions();
			option.setNodeName("M" + i);
			option.getAuthenticationOptions().setKeystorePassword("ABC");
			option.getSearchOptions().setStartEmbedded(false);
			option.getSearchOptions().setUrl(null);
			option.getHttpServerOptions().setPort(8000 + i);
			option.getMonitoringOptions().setEnabled(false);
			option.getMonitoringOptions().setPort(8500 + i);
			setupOptions(option, i);
			Mesh mesh = Mesh.create(option);
			mesh.run(false);
			System.out.println("Done");
			MeshComponent meshInternal = mesh.internal();
			meshInternal.database().tx(tx -> {
				UserDao userDao = tx.userDao();
				UserRoot userRoot = meshInternal.boot().userRoot();
				System.out.println(userRoot.getUuid());
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

	protected abstract void setupOptions(T option, int i);
}
